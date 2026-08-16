#include "main_window.h"
#include <QFileDialog>
#include <QMessageBox>
#include <QImage>
#include <QPixmap>
#include <QDir>
#include <cstring>
#include <cstdlib>
#include <algorithm>

MainWindow::MainWindow(QWidget *parent) : QMainWindow(parent), dag(nullptr) {
    setWindowTitle("PowerCut Desktop");
    resize(1280, 720);
    setupUi();
}

void MainWindow::setupUi() {
    QWidget *central = new QWidget;
    QHBoxLayout *mainLayout = new QHBoxLayout;

    QVBoxLayout *timelineLayout = new QVBoxLayout;
    timelineList = new QListWidget;
    timelineList->setFixedWidth(300);
    timelineLayout->addWidget(new QLabel("Timeline"));
    timelineLayout->addWidget(timelineList);

    QVBoxLayout *rightLayout = new QVBoxLayout;
    previewLabel = new QLabel;
    previewLabel->setAlignment(Qt::AlignCenter);
    previewLabel->setMinimumSize(640, 360);
    previewLabel->setStyleSheet("background: black; color: white;");
    previewLabel->setText("No preview");
    rightLayout->addWidget(previewLabel, 1);

    exportBtn = new QPushButton("Export via FFmpeg CLI");
    exportProgress = new QProgressBar;
    exportProgress->setRange(0, 100);
    exportProgress->setValue(0);
    rightLayout->addWidget(exportBtn);
    rightLayout->addWidget(exportProgress);

    mainLayout->addLayout(timelineLayout, 1);
    mainLayout->addLayout(rightLayout, 3);
    central->setLayout(mainLayout);
    setCentralWidget(central);

    QMenu *fileMenu = menuBar()->addMenu("File");
    QAction *openAct = fileMenu->addAction("Open Project");
    connect(openAct, &QAction::triggered, this, &MainWindow::openProject);
    fileMenu->addSeparator();
    QAction *exitAct = fileMenu->addAction("Exit");
    connect(exitAct, &QAction::triggered, this, &close);

    connect(exportBtn, &QPushButton::clicked, this, &MainWindow::exportVideo);
    connect(&exportMgr, &ExportManager::progress, this, &MainWindow::onExportProgress);
    connect(&exportMgr, &ExportManager::finished, this, &MainWindow::onExportFinished);
    connect(&renderer, &TimelineRenderer::frameReady, this, &MainWindow::onFrameRendered);

    statusBar()->showMessage("Ready");
}

void MainWindow::openProject() {
    QString file = QFileDialog::getOpenFileName(this, "Open Project", QString(), "PowerCut Projects (*.json)");
    if (file.isEmpty()) return;

    if (dag) {
        delete dag;
        dag = nullptr;
    }

    dag = importer.load(file.toStdString());
    if (!dag) {
        QMessageBox::critical(this, "Error", "Failed to load project");
        return;
    }

    currentProjectPath = file;
    timelineList->clear();
    auto segs = dag->evaluate(0);
    for (const auto &seg : segs) {
        timelineList->addItem(QString("Clip %1 (track %2, type %3)")
            .arg(seg.mat_id)
            .arg(seg.track_index)
            .arg(seg.track_type));
    }

    renderer.renderFrame(dag, 0, 640, 360);
    statusBar()->showMessage("Project loaded: " + file);
}

void MainWindow::exportVideo() {
    if (!dag) {
        QMessageBox::warning(this, "Warning", "No project loaded");
        return;
    }

    QString defaultName = "output.mp4";
    if (!currentProjectPath.isEmpty()) {
        QDir dir = QFileInfo(currentProjectPath).dir();
        defaultName = dir.filePath("output.mp4");
    }

    QString outFile = QFileDialog::getSaveFileName(this, "Export Video", defaultName, "MP4 (*.mp4)");
    if (outFile.isEmpty()) return;

    exportBtn->setEnabled(false);
    exportProgress->setValue(0);
    statusBar()->showMessage("Exporting via FFmpeg...");

    PowerCut::ExportConfig cfg;
    cfg.preset = PowerCut::ExportEngine::p_yt1080();
    cfg.out = outFile.toStdString();
    cfg.remove_watermark = true;

    exportMgr.startExport(dag, cfg);
}

void MainWindow::onExportProgress(const ExportProgress &p) {
    int pct = (int)(p.cur * 100.0 / std::max(1LL, p.total));
    exportProgress->setValue(pct);
    statusBar()->showMessage(QString("Exporting... %1%").arg(pct));
}

void MainWindow::onExportFinished(bool success) {
    exportBtn->setEnabled(true);
    if (success) {
        QMessageBox::information(this, "Export", "Export completed successfully!");
        statusBar()->showMessage("Export complete");
    } else {
        QMessageBox::critical(this, "Export", "Export failed. Ensure ffmpeg is installed and in PATH.");
        statusBar()->showMessage("Export failed");
    }
}

void MainWindow::onFrameRendered(const QImage &frame) {
    if (!frame.isNull()) {
        updatePreview(frame);
    }
}

void MainWindow::updatePreview(const QImage &frame) {
    QPixmap pm = QPixmap::fromImage(frame);
    previewLabel->setPixmap(pm.scaled(
        previewLabel->size(), Qt::KeepAspectRatio, Qt::SmoothTransformation));
}
