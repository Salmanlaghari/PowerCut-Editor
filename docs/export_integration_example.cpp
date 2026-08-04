#include "powercut/export/export_engine.h"
#include <memory>
void on_export_click(bool user_clicked_ad){
  using namespace PowerCut;
  auto ex=std::make_unique<ExportEngine>();
  ex->on_progress([](const ExportProgress&p){
    float pct=p.cur*100.f/std::max(1LL,p.total);
    // update_ui(pct, p.speed_x, p.eta_s);
  });
  ExportConfig c; c.preset=ExportEngine::p_tiktok();
  c.out="/sdcard/Movies/PowerCut/out.mp4";
  c.remove_watermark = user_clicked_ad; // ✅ true = ad clicked → no watermark
  ex->start(global_app->dag(),c);
}
// UI Logic:
// Show checkbox/button: "✅ Click Ad to Remove Watermark"
// If user clicks ad → pass true → clean export
// Else → pass false → PowerCut watermark auto added
