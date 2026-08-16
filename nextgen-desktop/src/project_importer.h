#pragma once
#include <QObject>
#include <QImage>
#include <string>
#include <QJsonObject>
#include <QJsonArray>
#include "powercut/core/dag.h"

class ProjectImporter : public QObject {
    Q_OBJECT
public:
    explicit ProjectImporter(QObject *parent = nullptr);
    PowerCut::PowerCutDAG* load(const std::string &path);

private:
    void applyVideoSettings(PowerCut::DAGSegment &seg, const QJsonObject &obj);
    void applyEffects(PowerCut::DAGSegment &seg, const QJsonObject &obj);
};
