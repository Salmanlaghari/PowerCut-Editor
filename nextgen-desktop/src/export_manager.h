#pragma once
#include <QObject>
#include <QProcess>
#include <string>
#include <atomic>
#include <vector>
#include <memory>
#include "powercut/export/export_engine.h"

struct ExportProgress {
    int64_t cur, total;
    double speed_x;
    int eta_s;
    size_t bytes;
};

class ExportManager : public QObject {
    Q_OBJECT
public:
    explicit ExportManager(QObject *parent = nullptr);
    ~ExportManager();
    void startExport(PowerCut::PowerCutDAG *dag, const PowerCut::ExportConfig &cfg);
    void cancel();

signals:
    void progress(const ExportProgress &p);
    void finished(bool success);

private:
    QString buildFilterGraph(const std::vector<PowerCut::DAGSegment> &segs, int w, int h) const;
    QImage renderFrame(PowerCut::PowerCutDAG *dag, int64_t timeMicros, int width, int height) const;

    std::atomic<bool> running;
    std::thread worker;
    std::unique_ptr<QProcess> ffmpegProcess;
};
