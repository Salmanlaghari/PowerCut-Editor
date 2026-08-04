#pragma once
#include <cstdint>
#include <string>
#include <atomic>
#include <functional>
#include <memory>
#include "powercut/core/dag.h"
#include "powercut/core/decoder_farm.h"
#include "powercut/core/compositor.h"
namespace PowerCut {
using TimeMicros = int64_t;
struct ExportPreset { std::string name; int w,h; double fps; int64_t tbr,mbr; std::string vcodec,acodec,container; };
struct ExportConfig {
  ExportPreset preset;
  std::string out;
  bool hw=true, two_pass=true, faststart=true;
  bool remove_watermark=false;
};
struct ExportProgress { int64_t cur,total; double speed_x; int eta_s; size_t bytes; };
class ExportEngine {
public:
  ExportEngine(); ~ExportEngine();
  bool start(PowerCutDAG* d, const ExportConfig& c); void cancel(); bool running() const;
  using Cb = std::function<void(const ExportProgress&)>; void on_progress(Cb f);
  static ExportPreset p_tiktok(){return{"TikTok",1080,1920,30.,10000000,15000000,"h264","aac","mp4"};}
  static ExportPreset p_reels(){return{"Reels",1080,1920,30.,10000000,15000000,"h264","aac","mp4"};}
  static ExportPreset p_shorts(){return{"Shorts",1080,1920,30.,10000000,15000000,"h264","aac","mp4"};}
  static ExportPreset p_yt1080(){return{"YT1080",1920,1080,30.,12000000,18000000,"h264","aac","mp4"};}
  static ExportPreset p_yt4k(){return{"YT4K",3840,2160,30.,45000000,70000000,"h264","aac","mp4"};}
  static ExportPreset p_wa(){return{"WhatsApp",720,1280,30.,4000000,6000000,"h264","aac","mp4"};}
  static ExportPreset p_hevc(){return{"ArchiveHEVC",1920,1080,30.,6000000,10000000,"hevc","aac","mp4"};}
  static ExportPreset p_prores(){return{"ProResHQ",3840,2160,30.,250000000,0,"prores_ks","pcm_s16le","mov"};}
private:
  struct Impl; std::unique_ptr<Impl> m;
  void worker(); bool setup_enc(); bool enc_v(RGBAFrame*); bool enc_a(PCMFrame*); bool mux();
  void apply_watermark(RGBAFrame* frame);
};
}
