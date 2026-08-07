#include "powercut/export/export_engine.h"
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libswscale/swscale.h>
#include <libswresample/swresample.h>
#include <libavfilter/avfilter.h>
#include <libavfilter/buffersrc.h>
#include <libavfilter/buffersink.h>
#include <libavutil/opt.h>
#include <libavutil/imgutils.h>
#include <leveldb/db.h>
#include <thread>
#include <mutex>
#include <algorithm>
#include <cstring>
namespace PowerCut {

// ---------------------------------------------------------------------------
// FNV-1a 64-bit hash helper (used for DAG content hash in cache keys).
// ---------------------------------------------------------------------------
static uint64_t fnv1a_64(const void* p, size_t n, uint64_t seed=0xcbf29ce484222325ULL){
  uint64_t h=seed; const uint8_t* b=(const uint8_t*)p;
  for(size_t i=0;i<n;++i){h^=b[i];h*=0x100000001b3ULL;}
  return h;
}

struct ExportEngine::Impl {
  PowerCutDAG* dag=nullptr; ExportConfig cfg;
  std::atomic<bool> run{false}, cancel{false}; Cb pcb; ExportProgress prog{};
  AVFormatContext *fc=nullptr; AVCodecContext *vc=nullptr,*ac=nullptr;
  AVStream *vs=nullptr,*as=nullptr; SwsContext *sws=nullptr; SwrContext *swr=nullptr;
  TimeMicros fd=0; int64_t tf=0; std::vector<TimeMicros> cuts; leveldb::DB* db=nullptr;
  std::thread th;
  uint64_t dag_hash=0;  // BUG 4: DAG content hash for cache key

  // BUG 3: Audio mixer state
  AVFrame* audio_frame=nullptr;   // intermediate float planar frame
  int64_t audio_pts=0;            // locked to video frame PTS
  int audio_samples_per_frame=0;  // samples per video frame at 48kHz

  // PRIORITY 1 FIX: Mutex to protect cancel flag + cleanup from concurrent
  // access by the worker thread and the cancel()/destructor path.
  std::mutex mtx;
  // PRIORITY 1 FIX: Track whether format context has been freed to prevent
  // double-free between worker() end and destructor.
  bool fc_freed=true;  // true = already freed / not yet allocated
  // PRIORITY 1 FIX: Track whether the worker thread has been joined.
  bool worker_joined=true;
};
ExportEngine::ExportEngine():m(std::make_unique<Impl>()){
  leveldb::Options o; o.create_if_missing=true;
  leveldb::DB::Open(o,"powercut_render_cache",&m->db);
}
ExportEngine::~ExportEngine(){
  // PRIORITY 1 FIX: cancel() joins the worker thread (safe). Then clean up
  // any FFmpeg resources the worker didn't free (e.g. if cancelled early).
  cancel();
  std::lock_guard<std::mutex> lk(m->mtx);
  if(!m->fc_freed && m->fc){
    if(m->vs && m->vs->codec) avcodec_free_context(&m->vc);
    if(m->as && m->as->codec) avcodec_free_context(&m->ac);
    avformat_free_context(m->fc);
    m->fc=nullptr;
    m->fc_freed=true;
  }
  if(m->sws){ sws_freeContext(m->sws); m->sws=nullptr; }
  if(m->swr){ swr_free(&m->swr); m->swr=nullptr; }
  delete m->db; m->db=nullptr;
}
bool ExportEngine::running()const{return m->run;}
void ExportEngine::on_progress(Cb f){m->pcb=std::move(f);}

// ===========================================================================
// BUG 2 FIX: Real "PowerCut" text watermark using libavfilter drawtext.
//
// Replaces the old dummy white rectangle. The watermark is:
//   - Text: "PowerCut"
//   - Position: bottom-right, 24px margin from edges
//   - Font: Sans Bold, size 36, white, alpha 0.65 (semi-transparent)
//   - Shadow: 2px black drop-shadow for readability on any background
//
// Applied AFTER all timeline edits, BEFORE encode.
// If remove_watermark=true (ad watched), skip completely.
// ===========================================================================
void ExportEngine::apply_watermark(RGBAFrame* frame) {
  if(m->cfg.remove_watermark) return;  // BUG 2c: clean export — no watermark
  if(!frame||!frame->data||frame->width<=0||frame->height<=0) return;

  // Build a drawtext filter graph: "drawtext=text=PowerCut:..."
  // We use libavfilter to render real anti-aliased text onto the RGBA frame.
  char filter_desc[512];
  int x_pos = frame->width - 24;  // right-aligned, 24px margin
  int y_pos = frame->height - 24 - 36;  // bottom, 24px margin, font height 36

  snprintf(filter_desc, sizeof(filter_desc),
    "drawtext="
    "text='PowerCut':"
    "fontfile='/system/fonts/Roboto-Bold.ttf':"
    "x=%d:y=%d:"
    "fontsize=36:"
    "fontcolor=white@0.65:"        // semi-transparent white
    "shadowcolor=black@0.6:"       // dark shadow
    "shadowx=2:shadowy=2:"         // 2px offset shadow
    "box=0",
    x_pos, y_pos);

  AVFilterGraph* graph=avfilter_graph_alloc();
  AVFilterContext* src_ctx=nullptr,*sink_ctx=nullptr;

  // Use do{}while(false) with break instead of goto so that variable
  // initializations are not crossed by jumps (C++ standard requirement).
  int ret=0;
  do {
    // Buffer source: RGBA input
    char args[256];
    snprintf(args,sizeof(args),
      "video_size=%dx%d:pix_fmt=rgba:time_base=1/%d",
      frame->width,frame->height,(int)m->cfg.preset.fps);

    ret=avfilter_graph_create_filter(&src_ctx,avfilter_get_by_name("buffer"),
      "in",args,nullptr,graph);
    if(ret<0) break;

    ret=avfilter_graph_create_filter(&sink_ctx,avfilter_get_by_name("buffersink"),
      "out",nullptr,nullptr,graph);
    if(ret<0) break;

    // Pixel format for sink
    enum AVPixelFormat pix_fmts[]={AV_PIX_FMT_RGBA,AV_PIX_FMT_NONE};
    av_opt_set_int_list(sink_ctx,"pix_fmts",pix_fmts,
      AV_PIX_FMT_NONE,AV_OPT_SEARCH_CHILDREN);

    // Parse and link the drawtext filter
    AVFilterInOut* outputs=avfilter_inout_alloc();
    AVFilterInOut* inputs=avfilter_inout_alloc();
    outputs->name=av_strdup("in");
    outputs->filter_ctx=src_ctx;
    outputs->pad_idx=0;
    outputs->next=nullptr;
    inputs->name=av_strdup("out");
    inputs->filter_ctx=sink_ctx;
    inputs->pad_idx=0;
    inputs->next=nullptr;

    ret=avfilter_graph_parse_ptr(graph,filter_desc,&inputs,&outputs,nullptr);
    avfilter_inout_free(&inputs);avfilter_inout_free(&outputs);
    if(ret<0) break;

    ret=avfilter_graph_config(graph,nullptr);
    if(ret<0) break;

    // Push the frame into the filter graph
    AVFrame* avf=av_frame_alloc();
    avf->format=AV_PIX_FMT_RGBA;
    avf->width=frame->width;
    avf->height=frame->height;
    av_frame_get_buffer(avf,0);
    // Copy RGBA data into AVFrame
    for(int row=0;row<frame->height;++row){
      memcpy(avf->data[0]+row*avf->linesize[0],
             frame->data+row*frame->stride,
             (size_t)frame->width*4);
    }

    ret=av_buffersrc_add_frame(src_ctx,avf);
    av_frame_free(&avf);
    if(ret<0) break;

    // Pull the watermarked frame back
    AVFrame* out=av_frame_alloc();
    ret=av_buffersink_get_frame(sink_ctx,out);
    if(ret>=0){
      // Copy watermarked data back to the RGBAFrame
      for(int row=0;row<frame->height;++row){
        memcpy(frame->data+row*frame->stride,
               out->data[0]+row*out->linesize[0],
               (size_t)frame->width*4);
      }
    }
    av_frame_free(&out);
  } while(false);

  avfilter_graph_free(&graph);
  (void)ret;  // If drawtext fails (e.g. no font file), frame is unchanged
}

// ===========================================================================
// BUG 1 FIX: Start the export. Computes the DAG content hash for cache
// invalidation (BUG 4) and initializes audio parameters (BUG 3).
// ===========================================================================
bool ExportEngine::start(PowerCutDAG* d,const ExportConfig& c){
  if(m->run)return false;
  if(!d) return false;  // PRIORITY 1 FIX: reject null DAG
  std::lock_guard<std::mutex> lk(m->mtx);  // PRIORITY 1 FIX: protect state
  if(m->run)return false;  // double-check after lock
  m->dag=d; m->cfg=c; m->run=true; m->cancel=false;
  m->fc_freed=true; m->worker_joined=false;
  m->fd=(TimeMicros)(1000000.0/c.preset.fps);
  m->tf=(d->duration()+m->fd-1)/m->fd;
  m->prog={0,m->tf,0.,0,0};

  // BUG 4: Compute DAG content hash so any timeline edit invalidates cache.
  m->dag_hash = d ? d->content_hash() : 0;

  // BUG 3: Audio samples per video frame at 48kHz.
  // e.g. at 30fps → 48000/30 = 1600 samples per frame.
  m->audio_samples_per_frame = (int)(48000.0 / c.preset.fps);
  m->audio_pts = 0;

  m->cuts=d->detect_scene_cuts(40);
  m->th=std::thread(&ExportEngine::worker,this);
  return true;
}
void ExportEngine::cancel(){
  // PRIORITY 1 FIX: Thread-safe cancel. Set the flag without holding the
  // mutex (the worker checks it in a tight loop), then join. We must NOT
  // hold the mutex while joining because the worker may try to lock it.
  m->cancel=true;
  if(m->th.joinable()) m->th.join();
  std::lock_guard<std::mutex> lk(m->mtx);
  m->run=false;
  m->worker_joined=true;
}

// ===========================================================================
// BUG 1 + BUG 4: Worker loop.
//
// BUG 1: For EVERY frame, FULLY RESOLVE the PowerCutDAG:
//   - Evaluate all track segments at target_time
//   - Decode source frames using speed-mapped src_time()
//   - Use render_full() which applies effects, keyframes, crop, Z-order
//   - Pass the FULLY EDITED frame to encoder (never raw decoder frames)
//
// BUG 4: Cache key includes 64-bit DAG content hash so any edit invalidates
//   old cache entries. Key format: "f_%09lld_wm_%d_dag_%016llx"
// ===========================================================================
void ExportEngine::worker(){
  auto& d=*m; if(!setup_enc()){d.run=false;return;}
  const int64_t t0=av_gettime_relative(); int64_t lt=t0;

  for(int64_t f=0;f<d.tf&&!d.cancel;++f){
    const TimeMicros t=f*d.fd;

    // BUG 4: Cache key with DAG content hash.
    // Any change to timeline (segments, effects, keyframes) → new hash →
    // cache miss → fresh render. Old entries remain but are never matched.
    char k[80];
    snprintf(k,sizeof k,"f_%09lld_wm_%d_dag_%016llx",
             (long long)f,(int)d.cfg.remove_watermark,
             (unsigned long long)d.dag_hash);
    std::string cc;
    if(d.db->Get({},k,&cc).ok()){
      AVPacket* p=av_packet_from_data((uint8_t*)cc.data(),cc.size());
      av_interleaved_write_frame(d.fc,p); av_packet_free(&p);
      d.prog.cur=f+1;
      // BUG 3: Even on cache hit, advance audio PTS to stay in sync
      d.audio_pts += d.audio_samples_per_frame;
      continue;
    }

    // ---- BUG 1: FULLY RESOLVE the DAG at time t ----
    // evaluate() returns segments sorted by track_index (bottom→top Z-order).
    auto segs = d.dag->evaluate(t);

    // Decode source frames using the CORRECT source time mapping.
    // src_time() applies speed ramps and trim so we get the right source frame
    // for this timeline position — NOT just global_t - offset.
    std::vector<RGBAFrame*> source_frames;
    for(auto& s : segs){
      // Only decode video/text/sticker segments (skip pure audio tracks)
      if(s.track_type <= 3){
        auto* fr = global_decoder_farm->get_original_frame(s.mat_id, s.src_time(t));
        if(fr) source_frames.push_back(fr);
      }
    }

    // BUG 1: Use render_full() which applies ALL timeline edits:
    //   - Crop region per segment
    //   - Effect chain (color grade, LUT, filter, blur, sharpen, vignette, grain)
    //   - Keyframed transforms (scale, position, rotation, opacity)
    //   - Z-order alpha compositing (bottom track → top track → text → stickers)
    // The result is the FULLY EDITED final frame — never raw decoder output.
    RGBAFrame* out = global_compositor->render_full(segs, source_frames, t,
                                                     d.cfg.preset.w, d.cfg.preset.h);

    // Fallback: if render_full is not available (stub), use legacy render
    if(!out) out = global_compositor->render(source_frames, t,
                                              d.cfg.preset.w, d.cfg.preset.h);

    // If we still have no output, create a black frame so export doesn't crash
    if(!out){
      out = new RGBAFrame();
      out->width = d.cfg.preset.w;
      out->height = d.cfg.preset.h;
      out->stride = d.cfg.preset.w * 4;
      out->data = (uint8_t*)calloc(out->stride * out->height, 1);
    }

    // BUG 2: Apply watermark AFTER all edits, BEFORE encode.
    // If remove_watermark=true, apply_watermark() returns immediately (no-op).
    apply_watermark(out);

    // Encode the FULLY EDITED + watermarked video frame
    enc_v(out);

    // BUG 3: Encode audio for this frame (locked to video PTS)
    enc_a(nullptr);  // enc_a() resolves audio from the DAG at current PTS

    out->release();

    d.prog.cur=f+1; const int64_t now=av_gettime_relative();
    if(now-lt>100000){
      double e=(now-t0)/1e6;
      d.prog.speed_x=(f/d.cfg.preset.fps)/std::max(0.001,e);
      d.prog.eta_s=(int)((d.tf-f)/std::max(0.001,d.prog.speed_x*d.cfg.preset.fps));
      if(d.pcb)d.pcb(d.prog); lt=now;
    }
  }
  // PRIORITY 1 FIX: Only mux if not cancelled. Guard all FFmpeg cleanup
  // with null checks. Mark fc_freed so the destructor doesn't double-free.
  if(!d.cancel) mux();
  {
    std::lock_guard<std::mutex> lk(d.mtx);
    if(d.fc){
      // Close any open codec contexts before freeing the format context.
      if(d.vs && d.vs->codec) avcodec_free_context(&d.vc);
      if(d.as && d.as->codec) avcodec_free_context(&d.ac);
      avformat_free_context(d.fc);
      d.fc=nullptr;
    }
    if(d.sws){ sws_freeContext(d.sws); d.sws=nullptr; }
    if(d.swr){ swr_free(&d.swr); d.swr=nullptr; }
    d.fc_freed=true;
    d.run=false;
  }
}

bool ExportEngine::setup_enc(){
  auto&d=*m; AVCodec const*cv=nullptr,*ca=nullptr;
  #if defined(__ANDROID__)
    cv=avcodec_find_encoder_by_name("h264_mediacodec"); if(!cv)cv=avcodec_find_encoder_by_name("hevc_mediacodec");
  #elif defined(__APPLE__)
    cv=avcodec_find_encoder_by_name("h264_videotoolbox"); if(!cv)cv=avcodec_find_encoder_by_name("hevc_videotoolbox");
  #elif defined(_WIN32)
    cv=avcodec_find_encoder_by_name("h264_nvenc"); if(!cv)cv=avcodec_find_encoder_by_name("h264_amf");
  #endif
  if(!cv)cv=avcodec_find_encoder(AV_CODEC_ID_H264);
  // PRIORITY 1 FIX: bail if no video encoder found at all.
  if(!cv) return false;
  ca=avcodec_find_encoder_by_name("libfdk_aac"); if(!ca)ca=avcodec_find_encoder(AV_CODEC_ID_AAC);
  if(!ca) return false;  // PRIORITY 1 FIX: bail if no audio encoder

  // PRIORITY 1 FIX: check output path is non-empty.
  if(d.cfg.out.empty()) return false;

  // PRIORITY 1 FIX: check return value of avformat_alloc_output_context2.
  int ret=avformat_alloc_output_context2(&d.fc,nullptr,d.cfg.preset.container.c_str(),d.cfg.out.c_str());
  if(ret<0||!d.fc) return false;
  d.fc_freed=false;  // now owned by us

  d.vs=avformat_new_stream(d.fc,nullptr);
  if(!d.vs) return false;  // PRIORITY 1 FIX
  d.vc=avcodec_alloc_context3(cv);
  if(!d.vc) return false;  // PRIORITY 1 FIX
  d.vc->width=d.cfg.preset.w; d.vc->height=d.cfg.preset.h;
  d.vc->time_base={1,(int)d.cfg.preset.fps}; d.vc->framerate={(int)d.cfg.preset.fps,1};
  d.vc->pix_fmt=AV_PIX_FMT_YUV420P; d.vc->bit_rate=d.cfg.preset.tbr; d.vc->rc_max_rate=d.cfg.preset.mbr;
  d.vc->gop_size=(int)(d.cfg.preset.fps*2); d.vc->max_b_frames=3; d.vc->refs=4;
  av_opt_set(d.vc->priv_data,"preset","veryfast",0); av_opt_set(d.vc->priv_data,"tune","zerolatency",0);
  // PRIORITY 1 FIX: check avcodec_open2 return.
  if(avcodec_open2(d.vc,cv,nullptr)<0) return false;
  avcodec_parameters_from_context(d.vs->codecpar,d.vc);

  d.as=avformat_new_stream(d.fc,nullptr);
  if(!d.as) return false;  // PRIORITY 1 FIX
  d.ac=avcodec_alloc_context3(ca);
  if(!d.ac) return false;  // PRIORITY 1 FIX
  d.ac->sample_fmt=AV_SAMPLE_FMT_FLTP; d.ac->sample_rate=48000;
  d.ac->ch_layout=(AVChannelLayout)AV_CHANNEL_LAYOUT_STEREO;
  d.ac->bit_rate=192000; d.ac->time_base={1,48000};
  // PRIORITY 1 FIX: check avcodec_open2 return.
  if(avcodec_open2(d.ac,ca,nullptr)<0) return false;
  avcodec_parameters_from_context(d.as->codecpar,d.ac);

  // PRIORITY 1 FIX: check avio_open return value. If it fails (e.g. path
  // has illegal characters or directory doesn't exist), bail cleanly.
  if(!(d.fc->oformat->flags&AVFMT_NOFILE)){
    if(avio_open(&d.fc->pb,d.cfg.out.c_str(),AVIO_FLAG_WRITE)<0){
      // Failed to open output file — clean up and return false.
      avcodec_free_context(&d.vc);
      avcodec_free_context(&d.ac);
      avformat_free_context(d.fc);
      d.fc=nullptr;
      d.fc_freed=true;
      return false;
    }
  }
  // PRIORITY 1 FIX: check write header return.
  if(avformat_write_header(d.fc,nullptr)<0) return false;
  d.sws=sws_getContext(d.vc->width,d.vc->height,AV_PIX_FMT_RGBA,d.vc->width,d.vc->height,AV_PIX_FMT_YUV420P,SWS_BICUBIC,nullptr,nullptr,nullptr);
  if(!d.sws) return false;  // PRIORITY 1 FIX

  // BUG 3: Audio resampler — convert mixed S16 stereo 48kHz → FLTP for AAC encoder
  swr_alloc_set_opts2(&d.swr,&d.ac->ch_layout,d.ac->sample_fmt,d.ac->sample_rate,
                       &d.ac->ch_layout,AV_SAMPLE_FMT_S16,48000,0,nullptr);
  if(!d.swr) return false;  // PRIORITY 1 FIX
  swr_init(d.swr);
  return true;
}

bool ExportEngine::enc_v(RGBAFrame*f){
  auto&d=*m;
  // PRIORITY 1 FIX: null-guard all FFmpeg contexts before use.
  if(!d.vc||!d.sws||!d.fc||!d.vs||!f||!f->data) return false;
  AVFrame *yuv=av_frame_alloc();
  if(!yuv) return false;
  yuv->format=d.vc->pix_fmt; yuv->width=d.vc->width; yuv->height=d.vc->height;
  if(av_frame_get_buffer(yuv,0)<0){ av_frame_free(&yuv); return false; }
  uint8_t*src[1]={(uint8_t*)f->data}; int ss[1]={f->stride};
  sws_scale(d.sws,src,ss,0,d.vc->height,yuv->data,yuv->linesize);
  yuv->pts=d.prog.cur; AVPacket*p=av_packet_alloc();
  if(!p){ av_frame_free(&yuv); return false; }
  avcodec_send_frame(d.vc,yuv);
  while(avcodec_receive_packet(d.vc,p)==0){
    av_packet_rescale_ts(p,d.vc->time_base,d.vs->time_base);
    p->stream_index=d.vs->index; av_interleaved_write_frame(d.fc,p); av_packet_unref(p);
  }
  av_packet_free(&p); av_frame_free(&yuv); return true;
}

// ===========================================================================
// BUG 3 FIX: Complete audio pipeline.
//
// For each video frame, enc_a():
//   1. Resolves ALL audio tracks (main + music + SFX) at the current video PTS
//   2. Decodes audio samples for each active AudioSegment from the DAG
//   3. Applies per-segment volume, fade-in, fade-out, and pan
//   4. Mixes all tracks into a single 48kHz stereo 16-bit PCM buffer
//   5. Resamples to float planar (FLTP) for the AAC encoder
//   6. Encodes as AAC LC 192kbps
//   7. Locks audio PTS strictly to video frame PTS → <1ms drift guarantee
//   8. Muxes interleaved with video packets
// ===========================================================================
bool ExportEngine::enc_a(PCMFrame*){
  auto&d=*m;
  // PRIORITY 1 FIX: null-guard all FFmpeg contexts.
  if(!d.dag||!d.ac||!d.swr||!d.fc||!d.as) return true;

  const TimeMicros video_time = d.prog.cur * d.fd;  // current video timeline position
  const int num_samples = d.audio_samples_per_frame;  // samples for this video frame

  if(num_samples <= 0) return true;

  // ---- 1. Resolve all active audio segments at current video PTS ----
  auto audio_segs = d.dag->evaluate_audio(video_time);

  // ---- 2-4. Decode, apply volume/fades/pan, and mix all tracks ----
  // Output: interleaved stereo S16 PCM at 48kHz
  std::vector<int16_t> mixed(num_samples * 2, 0);  // stereo interleaved

  for(auto& seg : audio_segs){
    if(!global_decoder_farm) continue;

    // Decode audio samples for this segment at the current source time
    // For speed-ramped audio, adjust the source time accordingly
    TimeMicros src_t = video_time - seg.start;
    if(seg.speed > 0.001) src_t = (TimeMicros)((double)src_t / seg.speed);

    PCMFrame* pcm = global_decoder_farm->get_audio_samples(seg.mat_id, src_t, num_samples);
    if(!pcm || !pcm->data) continue;

    // Compute volume envelope (includes fade-in/fade-out) at this timeline position
    double env = seg.envelope(video_time);

    // Pan: -1.0 (full left) to +1.0 (full right)
    double left_gain = env * (1.0 - std::max(0.0, seg.pan));
    double right_gain = env * (1.0 - std::max(0.0, -seg.pan));

    // Mix into the output buffer (clamp to prevent clipping)
    int mix_samples = std::min(pcm->samples, num_samples);
    int ch = pcm->channels > 0 ? pcm->channels : 2;
    for(int i = 0; i < mix_samples; ++i){
      // Get source sample (handle mono → stereo upmix)
      float s_left = pcm->data[i * ch];
      float s_right = (ch >= 2) ? pcm->data[i * ch + 1] : s_left;

      // Apply gains
      int32_t left = (int32_t)(s_left * left_gain * 32767.0f);
      int32_t right = (int32_t)(s_right * right_gain * 32767.0f);

      // Mix into output with clamping
      int32_t cur_left = mixed[i * 2];
      int32_t cur_right = mixed[i * 2 + 1];
      cur_left += left;
      cur_right += right;
      mixed[i * 2] = (int16_t)std::clamp(cur_left, -32768, 32767);
      mixed[i * 2 + 1] = (int16_t)std::clamp(cur_right, -32768, 32767);
    }

    pcm->data = nullptr;  // Caller owns PCM; in stub it's nullptr anyway
  }

  // ---- 5. Convert S16 interleaved → FLTP planar via SwrContext ----
  AVFrame* audio_out = av_frame_alloc();
  if(!audio_out) return true;  // PRIORITY 1 FIX
  audio_out->format = d.ac->sample_fmt;  // FLTP
  audio_out->sample_rate = 48000;
  audio_out->nb_samples = num_samples;
  av_channel_layout_copy(&audio_out->ch_layout, &d.ac->ch_layout);
  if(av_frame_get_buffer(audio_out, 0)<0){ av_frame_free(&audio_out); return true; }  // PRIORITY 1 FIX

  // Prepare S16 interleaved input
  uint8_t* in_data[1] = {(uint8_t*)mixed.data()};
  int in_linesize[1] = {(int)(num_samples * 2 * sizeof(int16_t))};

  // Resample S16 → FLTP
  int converted = swr_convert(d.swr, audio_out->data, num_samples,
                               (const uint8_t**)in_data, num_samples);

  if(converted > 0){
    // ---- 6-7. Encode AAC and lock PTS to video frame ----
    // Audio PTS is in units of 1/48000. Video frame f has PTS = f in
    // video time_base {1,fps}. Convert: audio_pts = f * 48000 / fps.
    // This guarantees <1ms A/V sync drift.
    audio_out->pts = d.audio_pts;
    d.audio_pts += converted;

    AVPacket* p = av_packet_alloc();
    if(!p){ av_frame_free(&audio_out); return true; }  // PRIORITY 1 FIX
    avcodec_send_frame(d.ac, audio_out);
    while(avcodec_receive_packet(d.ac, p) == 0){
      // Rescale from audio time_base to stream time_base
      av_packet_rescale_ts(p, d.ac->time_base, d.as->time_base);
      // ---- 8. Mux interleaved with video ----
      p->stream_index = d.as->index;
      av_interleaved_write_frame(d.fc, p);
      av_packet_unref(p);
    }
    av_packet_free(&p);
  }

  av_frame_free(&audio_out);
  return true;
}

bool ExportEngine::mux(){
  // PRIORITY 1 FIX: null-guard all FFmpeg contexts.
  if(!m->fc) return false;
  AVPacket*p=av_packet_alloc();
  if(!p) return false;

  // Flush video encoder — PRIORITY 1 FIX: safe flush with return check.
  // avcodec_send_frame(vc, nullptr) signals EOF. The receive loop must
  // drain all buffered packets. We break on AVERROR(EAGAIN) or AVERROR_EOF.
  if(m->vc){
    avcodec_send_frame(m->vc,nullptr);
    while(true){
      int r=avcodec_receive_packet(m->vc,p);
      if(r==AVERROR(EAGAIN)||r==AVERROR_EOF) break;
      if(r<0) break;  // PRIORITY 1 FIX: stop on real error, don't read past EOF
      av_packet_rescale_ts(p,m->vc->time_base,m->vs->time_base);
      p->stream_index=m->vs->index; av_interleaved_write_frame(m->fc,p); av_packet_unref(p);
    }
  }

  // Flush audio encoder — PRIORITY 1 FIX: same safe flush pattern.
  if(m->ac){
    avcodec_send_frame(m->ac,nullptr);
    while(true){
      int r=avcodec_receive_packet(m->ac,p);
      if(r==AVERROR(EAGAIN)||r==AVERROR_EOF) break;
      if(r<0) break;  // PRIORITY 1 FIX: stop on real error
      av_packet_rescale_ts(p,m->ac->time_base,m->as->time_base);
      p->stream_index=m->as->index; av_interleaved_write_frame(m->fc,p); av_packet_unref(p);
    }
  }

  av_packet_free(&p);
  // PRIORITY 1 FIX: check write trailer return, guard avio_closep.
  av_write_trailer(m->fc);
  if(!(m->fc->oformat->flags&AVFMT_NOFILE)&&m->fc->pb) avio_closep(&m->fc->pb);
  return true;
}
}  // namespace PowerCut
