#include "powercut/export/export_engine.h"
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libswscale/swscale.h>
#include <libswresample/swresample.h>
#include <leveldb/db.h>
#include <thread>
#include <mutex>
namespace PowerCut {
struct ExportEngine::Impl {
  PowerCutDAG* dag=nullptr; ExportConfig cfg;
  std::atomic<bool> run{false}, cancel{false}; Cb pcb; ExportProgress prog{};
  AVFormatContext *fc=nullptr; AVCodecContext *vc=nullptr,*ac=nullptr;
  AVStream *vs=nullptr,*as=nullptr; SwsContext *sws=nullptr; SwrContext *swr=nullptr;
  TimeMicros fd=0; int64_t tf=0; std::vector<TimeMicros> cuts; leveldb::DB* db=nullptr;
  std::thread th;
};
ExportEngine::ExportEngine():m(std::make_unique<Impl>()){
  leveldb::Options o; o.create_if_missing=true;
  leveldb::DB::Open(o,"powercut_render_cache",&m->db);
}
ExportEngine::~ExportEngine(){cancel(); delete m->db;}
bool ExportEngine::running()const{return m->run;}
void ExportEngine::on_progress(Cb f){m->pcb=std::move(f);}

void ExportEngine::apply_watermark(RGBAFrame* frame) {
  if(m->cfg.remove_watermark) return;
  int wm_w=220,wm_h=60;
  int x=frame->width-wm_w-24,y=frame->height-wm_h-24;
  uint8_t alpha=160;
  for(int py=y;py<y+wm_h&&py<frame->height;py++){
    for(int px=x;px<x+wm_w&&px<frame->width;px++){
      uint8_t* pix=frame->data+py*frame->stride+px*4;
      pix[0]=(pix[0]*(255-alpha)+255*alpha)/255;
      pix[1]=(pix[1]*(255-alpha)+255*alpha)/255;
      pix[2]=(pix[2]*(255-alpha)+255*alpha)/255;
      pix[3]=255;
    }
  }
}

bool ExportEngine::start(PowerCutDAG* d,const ExportConfig& c){
  if(m->run)return false; m->dag=d; m->cfg=c; m->run=true; m->cancel=false;
  m->fd=(TimeMicros)(1000000.0/c.preset.fps);
  m->tf=(d->duration()+m->fd-1)/m->fd;
  m->prog={0,m->tf,0.,0,0};
  m->cuts=d->detect_scene_cuts(40);
  m->th=std::thread(&ExportEngine::worker,this);
  return true;
}
void ExportEngine::cancel(){m->cancel=true;if(m->th.joinable())m->th.join();m->run=false;}
void ExportEngine::worker(){
  auto& d=*m; if(!setup_enc()){d.run=false;return;}
  const int64_t t0=av_gettime_relative(); int64_t lt=t0;
  for(int64_t f=0;f<d.tf&&!d.cancel;++f){
    const TimeMicros t=f*d.fd;
    char k[64]; snprintf(k,sizeof k,"f_%09lld_wm_%d",(long long)f,(int)d.cfg.remove_watermark);
    std::string cc;
    if(d.db->Get({},k,&cc).ok()){
      AVPacket* p=av_packet_from_data((uint8_t*)cc.data(),cc.size());
      av_interleaved_write_frame(d.fc,p); av_packet_free(&p);
      d.prog.cur=f+1; continue;
    }
    auto segs=d.dag->evaluate(t); std::vector<RGBAFrame*> sf;
    for(auto&s:segs){auto*fr=global_decoder_farm->get_original_frame(s.mat_id,s.src_time(t));if(fr)sf.push_back(fr);}
    RGBAFrame* out=global_compositor->render(sf,t,d.cfg.preset.w,d.cfg.preset.h);
    apply_watermark(out);
    enc_v(out); out->release();
    d.prog.cur=f+1; const int64_t now=av_gettime_relative();
    if(now-lt>100000){
      double e=(now-t0)/1e6;
      d.prog.speed_x=(f/d.cfg.preset.fps)/std::max(0.001,e);
      d.prog.eta_s=(int)((d.tf-f)/std::max(0.001,d.prog.speed_x*d.cfg.preset.fps));
      if(d.pcb)d.pcb(d.prog); lt=now;
    }
  }
  if(!d.cancel)mux(); avformat_free_context(d.fc); d.run=false;
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
  ca=avcodec_find_encoder_by_name("libfdk_aac"); if(!ca)ca=avcodec_find_encoder(AV_CODEC_ID_AAC);
  avformat_alloc_output_context2(&d.fc,nullptr,d.cfg.preset.container.c_str(),d.cfg.out.c_str());
  d.vs=avformat_new_stream(d.fc,nullptr);
  d.vc=avcodec_alloc_context3(cv);
  d.vc->width=d.cfg.preset.w; d.vc->height=d.cfg.preset.h;
  d.vc->time_base={1,(int)d.cfg.preset.fps}; d.vc->framerate={(int)d.cfg.preset.fps,1};
  d.vc->pix_fmt=AV_PIX_FMT_YUV420P; d.vc->bit_rate=d.cfg.preset.tbr; d.vc->rc_max_rate=d.cfg.preset.mbr;
  d.vc->gop_size=(int)(d.cfg.preset.fps*2); d.vc->max_b_frames=3; d.vc->refs=4;
  av_opt_set(d.vc->priv_data,"preset","veryfast",0); av_opt_set(d.vc->priv_data,"tune","zerolatency",0);
  avcodec_open2(d.vc,cv,nullptr); avcodec_parameters_from_context(d.vs->codecpar,d.vc);
  d.as=avformat_new_stream(d.fc,nullptr);
  d.ac=avcodec_alloc_context3(ca);
  d.ac->sample_fmt=AV_SAMPLE_FMT_FLTP; d.ac->sample_rate=48000; d.ac->ch_layout=(AVChannelLayout)AV_CHANNEL_LAYOUT_STEREO;
  d.ac->bit_rate=192000; d.ac->time_base={1,48000};
  avcodec_open2(d.ac,ca,nullptr); avcodec_parameters_from_context(d.as->codecpar,d.ac);
  if(!(d.fc->oformat->flags&AVFMT_NOFILE))avio_open(&d.fc->pb,d.cfg.out.c_str(),AVIO_FLAG_WRITE);
  avformat_write_header(d.fc,nullptr);
  d.sws=sws_getContext(d.vc->width,d.vc->height,AV_PIX_FMT_RGBA,d.vc->width,d.vc->height,AV_PIX_FMT_YUV420P,SWS_BICUBIC,nullptr,nullptr,nullptr);
  swr_alloc_set_opts2(&d.swr,&d.ac->ch_layout,d.ac->sample_fmt,d.ac->sample_rate,nullptr,AV_SAMPLE_FMT_S16,44100,0,nullptr); swr_init(d.swr);
  return true;
}
bool ExportEngine::enc_v(RGBAFrame*f){
  auto&d=*m; AVFrame *yuv=av_frame_alloc();
  yuv->format=d.vc->pix_fmt; yuv->width=d.vc->width; yuv->height=d.vc->height; av_frame_get_buffer(yuv,0);
  uint8_t*src[1]={(uint8_t*)f->data}; int ss[1]={f->stride};
  sws_scale(d.sws,src,ss,0,d.vc->height,yuv->data,yuv->linesize);
  yuv->pts=d.prog.cur; AVPacket*p=av_packet_alloc();
  avcodec_send_frame(d.vc,yuv);
  while(avcodec_receive_packet(d.vc,p)==0){
    av_packet_rescale_ts(p,d.vc->time_base,d.vs->time_base);
    p->stream_index=d.vs->index; av_interleaved_write_frame(d.fc,p); av_packet_unref(p);
  }
  av_packet_free(&p); av_frame_free(&yuv); return true;
}
bool ExportEngine::enc_a(PCMFrame*){return true;}
bool ExportEngine::mux(){
  avcodec_send_frame(m->vc,nullptr); AVPacket*p=av_packet_alloc();
  while(avcodec_receive_packet(m->vc,p)==0){
    av_packet_rescale_ts(p,m->vc->time_base,m->vs->time_base);
    p->stream_index=m->vs->index; av_interleaved_write_frame(m->fc,p); av_packet_unref(p);
  }
  av_packet_free(&p); av_write_trailer(m->fc);
  if(!(m->fc->oformat->flags&AVFMT_NOFILE))avio_closep(&m->fc->pb);
  return true;
}
}
