package com.diskmapper;

import javafx.concurrent.Task;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

public class DiskScanner extends Task<FileNode> {

    private final Path root;
    private final AtomicLong scannedCount = new AtomicLong(0);

    public DiskScanner(Path root) {
        this.root = root;
    }

    @Override
    protected FileNode call() throws Exception {
        updateMessage("スキャン中: " + root);
        FileNode rootNode = scanPath(root);
        rootNode.computeSize();
        return rootNode;
    }

    private FileNode scanPath(Path path) {
        if (isCancelled()) return null;

        boolean isDir = Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
        FileNode node = new FileNode(path.getFileName() != null ? path.getFileName().toString() : path.toString(),
                path.toString(), isDir);

        if (!isDir) {
            try {
                node.setSize(Files.size(path));
            } catch (IOException e) {
                node.setSize(0);
            }
            long count = scannedCount.incrementAndGet();
            if (count % 500 == 0) updateMessage("スキャン中: " + count + " ファイル処理済み");
            return node;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path child : stream) {
                if (isCancelled()) break;
                try {
                    if (!Files.isSymbolicLink(child)) {
                        FileNode childNode = scanPath(child);
                        if (childNode != null) node.addChild(childNode);
                    }
                } catch (Exception e) {
                    // skip inaccessible
                }
            }
        } catch (IOException e) {
            // skip inaccessible directory
        }

        long count = scannedCount.incrementAndGet();
        if (count % 100 == 0) updateMessage("スキャン中: " + count + " エントリ処理済み");

        return node;
    }
}
