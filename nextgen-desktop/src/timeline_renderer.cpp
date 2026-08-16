#include "timeline_renderer.h"
#include <QImage>
#include <cstring>
#include <cstdlib>

TimelineRenderer::TimelineRenderer(QObject *parent) : QObject(parent), comp(PowerCut::global_compositor) {
}

QImage TimelineRenderer::renderFrameSync(PowerCut::PowerCutDAG *dag, int64_t timeMicros, int width, int height) {
    if (!dag || !comp) {
        return QImage();
    }

    auto segs = dag->evaluate(timeMicros);
    std::vector<PowerCut::RGBAFrame*> sourceFrames;
    sourceFrames.reserve(segs.size());
    for (const auto &s : segs) {
        if (s.track_type <= 3) {
            PowerCut::RGBAFrame *fr = nullptr;
            if (PowerCut::global_decoder_farm) {
                fr = PowerCut::global_decoder_farm->get_original_frame(s.mat_id, s.src_time(timeMicros));
            }
            sourceFrames.push_back(fr);
        } else {
            sourceFrames.push_back(nullptr);
        }
    }

    PowerCut::RGBAFrame *out = comp->render_full(segs, sourceFrames, timeMicros, width, height);
    if (!out) {
        out = new PowerCut::RGBAFrame();
        out->width = width;
        out->height = height;
        out->stride = width * 4;
        out->data = (uint8_t*)calloc((size_t)out->stride * out->height, 1);
    }

    QImage img(out->data, width, height, out->stride, QImage::Format_RGBA8888);
    QImage copy = img.copy();

    if (out->data) free(out->data);
    delete out;
    for (auto fr : sourceFrames) {
        if (fr) {
            if (fr->data) free(fr->data);
            delete fr;
        }
    }

    return copy;
}

void TimelineRenderer::renderFrame(PowerCut::PowerCutDAG *dag, int64_t timeMicros, int width, int height) {
    QImage img = renderFrameSync(dag, timeMicros, width, height);
    if (!img.isNull()) {
        emit frameReady(img);
    }
}
