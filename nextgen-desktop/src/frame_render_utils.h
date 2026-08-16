#pragma once
#include <cstdint>
#include <QImage>
#include <vector>
#include "powercut/core/dag.h"

class FrameRenderUtils {
public:
    static QImage renderFrame(PowerCut::PowerCutDAG *dag, int64_t timeMicros, int width, int height);
};
