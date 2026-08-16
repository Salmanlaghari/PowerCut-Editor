#include "timeline_renderer.h"
#include <QImage>
#include <cstring>
#include <cstdlib>
#include "frame_render_utils.h"

TimelineRenderer::TimelineRenderer(QObject *parent) : QObject(parent), comp(PowerCut::global_compositor) {
}

QImage TimelineRenderer::renderFrameSync(PowerCut::PowerCutDAG *dag, int64_t timeMicros, int width, int height) {
    return FrameRenderUtils::renderFrame(dag, timeMicros, width, height);
}

void TimelineRenderer::renderFrame(PowerCut::PowerCutDAG *dag, int64_t timeMicros, int width, int height) {
    QImage img = renderFrameSync(dag, timeMicros, width, height);
    if (!img.isNull()) {
        emit frameReady(img);
    }
}
