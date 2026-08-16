#pragma once
#include <QMainWindow>
#include <QLabel>
#include <QListWidget>
#include <QPushButton>
#include <QProgressBar>
#include <QStatusBar>
#include <QAction>
#include <QImage>
#include "project_importer.h"
#include "timeline_renderer.h"
#include "export_manager.h"

class MainWindow : public QMainWindow {
    Q_OBJECT
public:
    explicit MainWindow(QWidget *parent = nullptr);

private slots:
    void openProject();
    void exportVideo();
    void onExportProgress(const ExportProgress &p);
    void onExportFinished(bool success);
    void onFrameRendered(const QImage &frame);

private:
    void setupUi();
    void updatePreview(const QImage &frame);

    QLabel *previewLabel;
    QListWidget *timelineList;
    QPushButton *exportBtn;
    QProgressBar *exportProgress;
    QStatusBar *statusBar;

    ProjectImporter importer;
    TimelineRenderer renderer;
    ExportManager exportMgr;

    PowerCut::PowerCutDAG *dag;
    QString currentProjectPath;
};
