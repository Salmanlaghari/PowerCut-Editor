#pragma once
#include <QObject>
#include <QImage>
#include <string>
#include "powercut/core/dag.h"
#include "powercut/core/compositor.h"
#include "powercut/core/decoder_farm.h"

class TimelineRenderer : public QObject {
    Q_OBJECT
public:
    explicit TimelineRenderer(QObject *parent = nullptr);
    QImage renderFrameSync(PowerCut::PowerCutDAG *dag, int64_t timeMicros, int width, int height);
    void renderFrame(PowerCut::PowerCutDAG *dag, int64_t timeMicros, int width, int height);

signals:
    void frameReady(const QImage &frame);

private:
    PowerCut::Compositor *comp;
};
