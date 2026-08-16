#include "project_importer.h"
#include <QFile>
#include <QJsonDocument>
#include <QJsonObject>
#include <QJsonArray>
#include <cstring>
#include <cstdlib>

ProjectImporter::ProjectImporter(QObject *parent) : QObject(parent) {
}

PowerCut::PowerCutDAG* ProjectImporter::load(const std::string &path) {
    QFile file(QString::fromStdString(path));
    if (!file.open(QIODevice::ReadOnly)) return nullptr;

    QByteArray data = file.readAll();
    QJsonDocument doc = QJsonDocument::fromJson(data);
    if (doc.isEmpty() || !doc.isObject()) return nullptr;

    QJsonObject obj = doc.object();
    auto *dag = new PowerCut::PowerCutDAG;

    int64_t durationMs = obj.value("durationMs").toVariant().toLongLong();
    dag->set_duration(durationMs * 1000);

    std::vector<PowerCut::DAGSegment> segs;
    std::vector<PowerCut::AudioSegment> audioSegs;

    PowerCut::DAGSegment seg;
    seg.mat_id = 1;
    seg.src_offset = 0;
    seg.track_index = 0;
    seg.track_type = 0;
    seg.speed = obj.value("speedFactor").toDouble(1.0);

    int64_t trimStart = obj.value("trimStartMs").toVariant().toLongLong();
    int64_t trimEnd = obj.value("trimEndMs").toVariant().toLongLong();
    seg.trim_start = trimStart * 1000;
    if (trimEnd > 0) seg.trim_end = trimEnd * 1000;

    seg.kf_scale.push_back({0, 1.0, PowerCut::Keyframe::LINEAR});
    seg.kf_opacity.push_back({0, 1.0, PowerCut::Keyframe::LINEAR});

    applyVideoSettings(seg, obj);
    applyEffects(seg, obj);

    QString imgPath = obj.value("imageOverlayPath").toString();
    if (!imgPath.isEmpty()) {
        PowerCut::DAGSegment overlaySeg;
        overlaySeg.mat_id = 2;
        overlaySeg.src_offset = 0;
        overlaySeg.track_index = 1;
        overlaySeg.track_type = 3;
        overlaySeg.kf_scale.push_back({0, obj.value("imageOverlayScale").toDouble(0.3), PowerCut::Keyframe::LINEAR});
        overlaySeg.kf_opacity.push_back({0, obj.value("imageOverlayOpacity").toDouble(1.0), PowerCut::Keyframe::LINEAR});
        segs.push_back(overlaySeg);
    }

    QString textOverlay = obj.value("activeTextOverlay").toString();
    if (!textOverlay.isEmpty()) {
        PowerCut::DAGSegment textSeg;
        textSeg.mat_id = 3;
        textSeg.src_offset = 0;
        textSeg.track_index = 2;
        textSeg.track_type = 1;
        textSeg.kf_scale.push_back({0, 1.0, PowerCut::Keyframe::LINEAR});
        textSeg.kf_opacity.push_back({0, 1.0, PowerCut::Keyframe::LINEAR});
        segs.push_back(textSeg);
    }

    QString stickerType = obj.value("stickerType").toString();
    if (!stickerType.isEmpty()) {
        PowerCut::DAGSegment stickerSeg;
        stickerSeg.mat_id = 4;
        stickerSeg.src_offset = 0;
        stickerSeg.track_index = 3;
        stickerSeg.track_type = 2;
        stickerSeg.kf_scale.push_back({0, 1.0, PowerCut::Keyframe::LINEAR});
        stickerSeg.kf_opacity.push_back({0, 1.0, PowerCut::Keyframe::LINEAR});
        segs.push_back(stickerSeg);
    }

    segs.push_back(seg);
    dag->set_segments(segs);

    QString bgmPath = obj.value("backgroundMusicPath").toString();
    if (!bgmPath.isEmpty()) {
        PowerCut::AudioSegment audioSeg;
        audioSeg.mat_id = 5;
        audioSeg.track_index = 1;
        audioSeg.start = 0;
        audioSeg.duration = durationMs * 1000;
        audioSeg.volume = obj.value("backgroundMusicVolume").toDouble(1.0);
        audioSeg.pan = 0.0;
        audioSeg.fade_in = 0;
        audioSeg.fade_out = 0;
        audioSeg.speed = 1.0;
        audioSegs.push_back(audioSeg);
    }
    dag->set_audio_segments(audioSegs);

    return dag;
}

void ProjectImporter::applyVideoSettings(PowerCut::DAGSegment &seg, const QJsonObject &obj) {
    double brightness = obj.value("imgBrightness").toDouble(0.0);
    double contrast = obj.value("imgContrast").toDouble(0.0);
    double saturation = obj.value("imgSaturation").toDouble(0.0);

    if (brightness > 0.001 || contrast > 0.001 || saturation > 0.001) {
        PowerCut::EffectNode eff;
        eff.name = "color_grade";
        eff.type = PowerCut::EffectNode::COLOR_GRADE;
        eff.intensity = 1.0;
        eff.params.push_back({0, brightness, PowerCut::Keyframe::LINEAR});
        seg.effects.push_back(eff);
    }

    double blur = obj.value("imgBlur").toDouble(0.0);
    if (blur > 0.001) {
        PowerCut::EffectNode eff;
        eff.name = "blur";
        eff.type = PowerCut::EffectNode::BLUR;
        eff.intensity = blur;
        seg.effects.push_back(eff);
    }

    double sharpen = obj.value("imgSharpen").toDouble(0.0);
    if (sharpen > 0.001) {
        PowerCut::EffectNode eff;
        eff.name = "sharpen";
        eff.type = PowerCut::EffectNode::SHARPEN;
        eff.intensity = sharpen;
        seg.effects.push_back(eff);
    }

    double vignette = obj.value("imgVignette").toDouble(0.0);
    if (vignette > 0.001) {
        PowerCut::EffectNode eff;
        eff.name = "vignette";
        eff.type = PowerCut::EffectNode::VIGNETTE;
        eff.intensity = vignette;
        seg.effects.push_back(eff);
    }

    double grain = obj.value("imgGrain").toDouble(0.0);
    if (grain > 0.001) {
        PowerCut::EffectNode eff;
        eff.name = "grain";
        eff.type = PowerCut::EffectNode::GRAIN;
        eff.intensity = grain;
        seg.effects.push_back(eff);
    }
}

void ProjectImporter::applyEffects(PowerCut::DAGSegment &seg, const QJsonObject &obj) {
    QString selectedFilter = obj.value("selectedFilter").toString();
    if (!selectedFilter.isEmpty()) {
        PowerCut::EffectNode eff;
        eff.name = selectedFilter.toStdString();
        if (selectedFilter.contains("sepia") || selectedFilter.contains("grayscale") || selectedFilter.contains("mono")) {
            eff.type = PowerCut::EffectNode::FILTER;
        } else if (selectedFilter.contains("blur")) {
            eff.type = PowerCut::EffectNode::BLUR;
        } else if (selectedFilter.contains("vignette")) {
            eff.type = PowerCut::EffectNode::VIGNETTE;
        } else if (selectedFilter.contains("sharpen")) {
            eff.type = PowerCut::EffectNode::SHARPEN;
        } else if (selectedFilter.contains("grain")) {
            eff.type = PowerCut::EffectNode::GRAIN;
        } else {
            eff.type = PowerCut::EffectNode::FILTER;
        }
        eff.intensity = 1.0;
        seg.effects.push_back(eff);
    }

    QString selectedEffect = obj.value("selectedEffect").toString();
    if (!selectedEffect.isEmpty() && selectedEffect != selectedFilter) {
        PowerCut::EffectNode eff;
        eff.name = selectedEffect.toStdString();
        eff.type = PowerCut::EffectNode::FILTER;
        eff.intensity = 1.0;
        seg.effects.push_back(eff);
    }

    if (obj.value("greenScreenEnabled").toBool()) {
        PowerCut::EffectNode eff;
        eff.name = "chroma_key_green";
        eff.type = PowerCut::EffectNode::FILTER;
        eff.intensity = obj.value("greenScreenThreshold").toDouble(0.4);
        seg.effects.push_back(eff);
    }
}
