package com.diskmapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Squarified TreeMap layout algorithm.
 */
public class TreeMapLayout {

    public record Rect(double x, double y, double w, double h, FileNode node) {}

    public List<Rect> layout(FileNode node, double x, double y, double w, double h) {
        List<FileNode> children = new ArrayList<>(node.getChildren());
        children.removeIf(c -> c.getSize() <= 0);
        children.sort(Comparator.comparingLong(FileNode::getSize).reversed());

        List<Rect> result = new ArrayList<>();
        squarify(children, x, y, w, h, node.getSize(), result);
        return result;
    }

    private void squarify(List<FileNode> nodes, double x, double y, double w, double h,
                          long totalSize, List<Rect> result) {
        if (nodes.isEmpty() || totalSize <= 0 || w <= 0 || h <= 0) return;

        List<FileNode> row = new ArrayList<>();
        double area = w * h;
        long usedSize = 0;

        int i = 0;
        while (i < nodes.size()) {
            List<FileNode> candidate = new ArrayList<>(row);
            candidate.add(nodes.get(i));

            if (row.isEmpty() || worstRatio(candidate, area, totalSize, w, h) <=
                    worstRatio(row, area, totalSize, w, h)) {
                row = candidate;
                usedSize += nodes.get(i).getSize();
                i++;
            } else {
                break;
            }
        }

        double[] bounds = layoutRow(row, x, y, w, h, totalSize, area, result);
        List<FileNode> remaining = nodes.subList(i, nodes.size());
        squarify(remaining, bounds[0], bounds[1], bounds[2], bounds[3], totalSize - usedSize, result);
    }

    private double worstRatio(List<FileNode> row, double area, long totalSize, double w, double h) {
        if (row.isEmpty()) return Double.MAX_VALUE;
        long rowSize = row.stream().mapToLong(FileNode::getSize).sum();
        double rowArea = area * rowSize / totalSize;
        double side = Math.min(w, h);
        double worst = 0;
        for (FileNode n : row) {
            double nodeArea = area * n.getSize() / totalSize;
            double r = Math.max((side * side * nodeArea) / (rowArea * rowArea),
                                (rowArea * rowArea) / (side * side * nodeArea));
            if (r > worst) worst = r;
        }
        return worst;
    }

    private double[] layoutRow(List<FileNode> row, double x, double y, double w, double h,
                               long totalSize, double area, List<Rect> result) {
        long rowSize = row.stream().mapToLong(FileNode::getSize).sum();
        double rowFraction = (double) rowSize / totalSize;

        boolean horizontal = w >= h;
        double rowLen = horizontal ? w * rowFraction : h * rowFraction;
        double pos = horizontal ? y : x;

        for (FileNode node : row) {
            double frac = (double) node.getSize() / rowSize;
            double len = (horizontal ? h : w) * frac;
            if (horizontal) {
                result.add(new Rect(x, pos, rowLen, len, node));
            } else {
                result.add(new Rect(pos, y, len, rowLen, node));
            }
            pos += len;
        }

        if (horizontal) return new double[]{x + rowLen, y, w - rowLen, h};
        else            return new double[]{x, y + rowLen, w, h - rowLen};
    }
}
