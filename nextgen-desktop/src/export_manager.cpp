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
            QImage frame = renderFrame(dag, t, w, h);
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

        QProcess ffmpeg;
        ffmpeg.setProgram(ffmpegPath);
        ffmpeg.setArguments(args);
        ffmpeg.setProcessChannelMode(QProcess::MergedChannels);

        ffmpeg.start();
        if (!ffmpeg.waitForStarted(5000)) {
            emit finished(false);
            running = false;
            return;
        }

        ffmpeg.waitForFinished(-1);
        bool success = (ffmpeg.exitCode() == 0 && ffmpeg.exitStatus() == QProcess::NormalExit);
        emit finished(success);
        running = false;
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

QImage ExportManager::renderFrame(PowerCut::PowerCutDAG *dag, int64_t timeMicros, int width, int height) const {
    if (!dag || !PowerCut::global_compositor) {
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

    PowerCut::RGBAFrame *out = PowerCut::global_compositor->render_full(segs, sourceFrames, timeMicros, width, height);
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

void ExportManager::cancel() {
    running = false;
}
