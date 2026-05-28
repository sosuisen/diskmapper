package com.diskmapper;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

public class MainController {

    private final Stage stage;
    private final BorderPane root = new BorderPane();
    private final TreeMapCanvas canvas;
    private final Label statusLabel = new Label("フォルダを選択してスキャンを開始してください");
    private final Label breadcrumbLabel = new Label("/");
    private final Label infoLabel = new Label("");
    private final Button backButton = new Button("← 戻る");
    private final Button scanButton = new Button("フォルダを選択...");
    private final ProgressBar progressBar = new ProgressBar();
    private final Deque<FileNode> history = new ArrayDeque<>();
    private DiskScanner activeScanner;

    public MainController(Stage stage) {
        this.stage = stage;
        canvas = new TreeMapCanvas(800, 550);
        buildUI();
    }

    private void buildUI() {
        root.setStyle("-fx-background-color: #1e1e2e;");

        // Top toolbar
        HBox toolbar = new HBox(8);
        toolbar.setPadding(new Insets(8, 12, 8, 12));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #2a2a3e;");

        scanButton.setStyle("-fx-background-color: #4e79a7; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        backButton.setStyle("-fx-background-color: #555577; -fx-text-fill: white; -fx-cursor: hand;");
        backButton.setDisable(true);

        breadcrumbLabel.setStyle("-fx-text-fill: #aaaacc; -fx-font-size: 12;");
        breadcrumbLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(breadcrumbLabel, Priority.ALWAYS);

        progressBar.setVisible(false);
        progressBar.setPrefWidth(150);
        progressBar.setStyle("-fx-accent: #4e79a7;");

        toolbar.getChildren().addAll(scanButton, backButton, breadcrumbLabel, progressBar);
        root.setTop(toolbar);

        // Canvas container
        StackPane canvasPane = new StackPane(canvas);
        canvasPane.setStyle("-fx-background-color: #1e1e2e;");
        root.setCenter(canvasPane);

        canvasPane.widthProperty().addListener((obs, o, n) -> canvas.resize(n.doubleValue(), canvas.getHeight()));
        canvasPane.heightProperty().addListener((obs, o, n) -> canvas.resize(canvas.getWidth(), n.doubleValue()));

        // Bottom status bar
        HBox statusBar = new HBox(16);
        statusBar.setPadding(new Insets(4, 12, 4, 12));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color: #2a2a3e;");

        statusLabel.setStyle("-fx-text-fill: #aaaacc; -fx-font-size: 11;");
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        infoLabel.setStyle("-fx-text-fill: #ccccaa; -fx-font-size: 11;");
        statusBar.getChildren().addAll(statusLabel, infoLabel);
        root.setBottom(statusBar);

        // Wire events
        scanButton.setOnAction(e -> chooseAndScan());
        backButton.setOnAction(e -> navigateBack());
        canvas.setOnDrillDown(this::drillDown);
        canvas.setOnHover(node -> {
            if (node != null) {
                infoLabel.setText(node.getName() + "  " + node.getFormattedSize());
            } else {
                infoLabel.setText("");
            }
        });
    }

    private void chooseAndScan() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("スキャンするフォルダを選択");
        File selected = chooser.showDialog(stage);
        if (selected == null) return;

        if (activeScanner != null) activeScanner.cancel();
        history.clear();
        backButton.setDisable(true);

        startScan(selected.toPath());
    }

    private void startScan(Path path) {
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        scanButton.setDisable(true);
        statusLabel.setText("スキャン中...");

        activeScanner = new DiskScanner(path);
        activeScanner.messageProperty().addListener((obs, o, msg) ->
                Platform.runLater(() -> statusLabel.setText(msg)));

        activeScanner.setOnSucceeded(e -> {
            FileNode result = activeScanner.getValue();
            Platform.runLater(() -> {
                progressBar.setVisible(false);
                scanButton.setDisable(false);
                statusLabel.setText("スキャン完了: " + result.getFormattedSize() + "  (" + result.getPath() + ")");
                breadcrumbLabel.setText(result.getPath());
                canvas.render(result);
            });
        });

        activeScanner.setOnFailed(e -> Platform.runLater(() -> {
            progressBar.setVisible(false);
            scanButton.setDisable(false);
            statusLabel.setText("エラー: " + activeScanner.getException().getMessage());
        }));

        activeScanner.setOnCancelled(e -> Platform.runLater(() -> {
            progressBar.setVisible(false);
            scanButton.setDisable(false);
        }));

        Thread t = new Thread(activeScanner, "disk-scanner");
        t.setDaemon(true);
        t.start();
    }

    private void drillDown(FileNode node) {
        if (canvas.getCurrentRoot() != null) history.push(canvas.getCurrentRoot());
        backButton.setDisable(false);
        breadcrumbLabel.setText(node.getPath());
        canvas.render(node);
        statusLabel.setText(node.getName() + "  " + node.getFormattedSize());
    }

    private void navigateBack() {
        if (history.isEmpty()) return;
        FileNode prev = history.pop();
        backButton.setDisable(history.isEmpty());
        breadcrumbLabel.setText(prev.getPath());
        canvas.render(prev);
        statusLabel.setText(prev.getName() + "  " + prev.getFormattedSize());
    }

    public BorderPane getRoot() { return root; }
}
