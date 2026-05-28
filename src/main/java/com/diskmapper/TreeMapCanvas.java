package com.diskmapper;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TreeMapCanvas extends Canvas {

    private static final double PAD = 1.5;
    private static final Font LABEL_FONT = Font.font("SansSerif", 11);
    private static final Font SMALL_FONT = Font.font("SansSerif", 9);

    private static final Color[] PALETTE = {
        Color.web("#4e79a7"), Color.web("#f28e2b"), Color.web("#e15759"),
        Color.web("#76b7b2"), Color.web("#59a14f"), Color.web("#edc948"),
        Color.web("#b07aa1"), Color.web("#ff9da7"), Color.web("#9c755f"),
        Color.web("#bab0ac")
    };

    private final TreeMapLayout layoutEngine = new TreeMapLayout();
    private List<TreeMapLayout.Rect> rects = new ArrayList<>();
    private FileNode currentRoot;
    private Consumer<FileNode> onDrillDown;
    private Consumer<FileNode> onHover;
    private TreeMapLayout.Rect hoveredRect;

    public TreeMapCanvas(double w, double h) {
        super(w, h);
        setOnMouseMoved(this::handleMouseMoved);
        setOnMouseClicked(this::handleMouseClicked);
    }

    public void setOnDrillDown(Consumer<FileNode> handler) { this.onDrillDown = handler; }
    public void setOnHover(Consumer<FileNode> handler) { this.onHover = handler; }

    public void render(FileNode node) {
        this.currentRoot = node;
        this.hoveredRect = null;
        rects = layoutEngine.layout(node, PAD, PAD, getWidth() - PAD * 2, getHeight() - PAD * 2);
        draw();
    }

    private void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
        gc.setFill(Color.web("#1e1e2e"));
        gc.fillRect(0, 0, getWidth(), getHeight());

        for (int i = 0; i < rects.size(); i++) {
            TreeMapLayout.Rect r = rects.get(i);
            Color base = PALETTE[i % PALETTE.length];
            boolean hovered = r == hoveredRect;

            gc.setFill(hovered ? base.brighter() : base);
            gc.fillRect(r.x(), r.y(), r.w(), r.h());

            gc.setStroke(Color.web("#1e1e2e"));
            gc.setLineWidth(PAD);
            gc.strokeRect(r.x(), r.y(), r.w(), r.h());

            drawLabel(gc, r);
        }
    }

    private void drawLabel(GraphicsContext gc, TreeMapLayout.Rect r) {
        if (r.w() < 20 || r.h() < 12) return;
        gc.setFill(Color.WHITE);
        gc.setFont(r.w() > 60 ? LABEL_FONT : SMALL_FONT);

        String name = r.node().getName();
        String size = r.node().getFormattedSize();

        double maxChars = r.w() / 7.0;
        if (name.length() > maxChars) name = name.substring(0, Math.max(1, (int) maxChars - 1)) + "…";

        gc.fillText(name, r.x() + 3, r.y() + 12);
        if (r.h() > 24) {
            gc.setFont(SMALL_FONT);
            gc.setFill(Color.web("#cccccc"));
            gc.fillText(size, r.x() + 3, r.y() + 24);
        }
    }

    private void handleMouseMoved(MouseEvent e) {
        TreeMapLayout.Rect found = findRect(e.getX(), e.getY());
        if (found != hoveredRect) {
            hoveredRect = found;
            draw();
            if (onHover != null) onHover.accept(found != null ? found.node() : null);
        }
    }

    private void handleMouseClicked(MouseEvent e) {
        TreeMapLayout.Rect found = findRect(e.getX(), e.getY());
        if (found != null && found.node().isDirectory() && onDrillDown != null) {
            onDrillDown.accept(found.node());
        }
    }

    private TreeMapLayout.Rect findRect(double mx, double my) {
        for (TreeMapLayout.Rect r : rects) {
            if (mx >= r.x() && mx <= r.x() + r.w() && my >= r.y() && my <= r.y() + r.h()) return r;
        }
        return null;
    }

    public FileNode getCurrentRoot() { return currentRoot; }

    public void resize(double w, double h) {
        setWidth(w);
        setHeight(h);
        if (currentRoot != null) render(currentRoot);
    }
}
