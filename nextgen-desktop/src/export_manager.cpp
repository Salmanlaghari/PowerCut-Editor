#include "export_manager.h"
#include <QProcess>
#include <QDir>
#include <QTemporaryDir>
#include <QImage>
#include <QFileInfo>
#include <thread>
#include <chrono>
#include <cstring>
#include <cstdlib>
#include <cmath>
#include "frame_render_utils.h"

ExportManager::ExportManager(QObject *parent) : QObject(parent), running(false) {
}

ExportManager::~ExportManager() {
    cancel();
    if (worker.joinable()) {
        worker.join();
    }
}

void ExportManager::startExport(PowerCut::PowerCutDAG *dag, const PowerCut::ExportConfig &cfg) {
    if (running) return;
    running = true;

    worker = std::thread([this, dag, cfg]() {
        int w = cfg.preset.w;
        int h = cfg.preset.h;
        double fps = cfg.preset.fps;
        int64_t durationMicros = dag->duration();
        int totalFrames = (int)std::round((double)durationMicros / 1000000.0 * fps) + 1;
        if (totalFrames <= 0) totalFrames = 1;

        QTemporaryDir tempDir;
        if (!tempDir.isValid()) {
            emit finished(false);
            running = false;
            return;
        }

        QString tempPath = tempDir.path();
        for (int i = 0; i < totalFrames && running; ++i) {
            int64_t t = (int64_t)((double)i / fps * 1000000.0);
            QImage frame = FrameRenderUtils::renderFrame(dag, t, w, h);
            if (frame.isNull()) continue;

            QString path = tempPath + QString("/frame_%1.png").arg(i, 6, 10, QChar('0'));
            frame.save(path, "PNG");

            ExportProgress ep;
            ep.cur = i + 1;
            ep.total = totalFrames;
            ep.speed_x = 0.0;
            ep.eta_s = 0;
            ep.bytes = 0;
            emit progress(ep);
        }

        if (!running) {
            emit finished(false);
            running = false;
            return;
        }

        auto segs = dag->evaluate(0);
        QString filterGraph = buildFilterGraph(segs, w, h);

        QString ffmpegPath = "ffmpeg";
        QStringList args;
        args << "-y"
             << "-framerate" << QString::number(fps)
             << "-i" << tempPath + "/frame_%06d.png"
             << "-vf" << filterGraph
             << "-c:v" << "libx264"
             << "-pix_fmt" << "yuv420p"
             << "-crf" << "18"
             << "-preset" << "fast"
             << QString::fromStdString(cfg.out);

        ffmpegProcess = std::make_unique<QProcess>();
        ffmpegProcess->setProgram(ffmpegPath);
        ffmpegProcess->setArguments(args);
        ffmpegProcess->setProcessChannelMode(QProcess::MergedChannels);

        ffmpegProcess->start();
        if (!ffmpegProcess->waitForStarted(5000)) {
            emit finished(false);
            running = false;
            ffmpegProcess.reset();
            return;
        }

        while (ffmpegProcess->state() == QProcess::Running && running) {
            ffmpegProcess->waitForFinished(200);
        }

        if (!running) {
            if (ffmpegProcess && ffmpegProcess->state() == QProcess::Running) {
                ffmpegProcess->kill();
                ffmpegProcess->waitForFinished(3000);
            }
            emit finished(false);
            running = false;
            ffmpegProcess.reset();
            return;
        }

        bool success = (ffmpegProcess->exitCode() == 0 && ffmpegProcess->exitStatus() == QProcess::NormalExit);
        emit finished(success);
        running = false;
        ffmpegProcess.reset();
    });
}

QString ExportManager::buildFilterGraph(const std::vector<PowerCut::DAGSegment> &segs, int w, int h) const {
    QStringList filters;

    for (const auto &seg : segs) {
        for (const auto &eff : seg.effects) {
            float fi = (float)eff.intensity;
            switch (eff.type) {
                case PowerCut::EffectNode::COLOR_GRADE:
                    filters << QString("eq=brightness=%1:contrast=%2:saturation=%3")
                        .arg(fi * 0.1, 0, 'f', 2)
                        .arg(1.0 + fi * 0.5, 0, 'f', 2)
                        .arg(1.0 + fi * 0.3, 0, 'f', 2);
                    break;
                case PowerCut::EffectNode::BLUR:
                    if (fi > 0.001f) {
                        filters << QString("boxblur=%1:1").arg((int)(fi * 4.0f));
                    }
                    break;
                case PowerCut::EffectNode::VIGNETTE:
                    filters << QString("vignette=PI/4:%1").arg(fi * 0.8, 0, 'f', 2);
                    break;
                case PowerCut::EffectNode::FILTER:
                    if (eff.name.find("sepia") != std::string::npos) {
                        filters << "colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131:0";
                    } else if (eff.name.find("grayscale") != std::string::npos ||
                               eff.name.find("mono") != std::string::npos) {
                        filters << "hue=s=0";
                    }
                    break;
                case PowerCut::EffectNode::SHARPEN:
                    if (fi > 0.001f) {
                        filters << QString("unsharp=5:5:%1:5:5:0.0").arg(fi * 0.5, 0, 'f', 2);
                    }
                    break;
                case PowerCut::EffectNode::GRAIN:
                    if (fi > 0.001f) {
                        filters << QString("noise=alls=%1:allf=t").arg((int)(fi * 40.0f));
                    }
                    break;
                default:
                    break;
            }
        }
    }

    if (filters.isEmpty()) return "null";
    return filters.join(",");
}

void ExportManager::cancel() {
    running = false;
    if (ffmpegProcess && ffmpegProcess->state() == QProcess::Running) {
        ffmpegProcess->kill();
    }
}
