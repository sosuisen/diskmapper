package com.diskmapper;

import java.util.ArrayList;
import java.util.List;

public class FileNode {
    private final String name;
    private final String path;
    private final boolean directory;
    private long size;
    private final List<FileNode> children = new ArrayList<>();
    private FileNode parent;

    public FileNode(String name, String path, boolean directory) {
        this.name = name;
        this.path = path;
        this.directory = directory;
    }

    public void addChild(FileNode child) {
        child.parent = this;
        children.add(child);
    }

    public void computeSize() {
        if (directory) {
            size = children.stream().mapToLong(c -> {
                c.computeSize();
                return c.size;
            }).sum();
        }
    }

    public String getName() { return name; }
    public String getPath() { return path; }
    public boolean isDirectory() { return directory; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public List<FileNode> getChildren() { return children; }
    public FileNode getParent() { return parent; }

    public String getFormattedSize() {
        if (size >= 1_073_741_824L) return String.format("%.2f GB", size / 1_073_741_824.0);
        if (size >= 1_048_576L)     return String.format("%.2f MB", size / 1_048_576.0);
        if (size >= 1_024L)         return String.format("%.2f KB", size / 1_024.0);
        return size + " B";
    }
}
