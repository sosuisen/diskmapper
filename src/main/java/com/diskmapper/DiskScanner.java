package com.diskmapper;

import javafx.concurrent.Task;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicLong;

public class DiskScanner extends Task<FileNode> {

    private final Path root;
    private final AtomicLong scannedCount = new AtomicLong(0);

    // Threshold: directories with fewer children are scanned sequentially
    private static final int PARALLEL_THRESHOLD = 4;

    public DiskScanner(Path root) {
        this.root = root;
    }

    @Override
    protected FileNode call() throws Exception {
        updateMessage("スキャン中: " + root);
        int threads = Runtime.getRuntime().availableProcessors();
        ForkJoinPool pool = new ForkJoinPool(threads);
        try {
            FileNode result = pool.invoke(new ScanTask(root));
            if (result != null) result.computeSize();
            return result;
        } finally {
            pool.shutdown();
        }
    }

    private class ScanTask extends RecursiveTask<FileNode> {

        private final Path path;

        ScanTask(Path path) {
            this.path = path;
        }

        @Override
        protected FileNode compute() {
            if (isCancelled()) return null;

            boolean isDir = Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
            String name = path.getFileName() != null ? path.getFileName().toString() : path.toString();
            FileNode node = new FileNode(name, path.toString(), isDir);

            if (!isDir) {
                try {
                    node.setSize(Files.size(path));
                } catch (IOException e) {
                    node.setSize(0);
                }
                long count = scannedCount.incrementAndGet();
                if (count % 1000 == 0) updateMessage("スキャン中: " + count + " ファイル処理済み");
                return node;
            }

            List<Path> children = listChildren(path);
            if (children.isEmpty()) return node;

            if (children.size() < PARALLEL_THRESHOLD) {
                // Small directory: scan sequentially in current thread
                for (Path child : children) {
                    if (isCancelled()) break;
                    FileNode childNode = new ScanTask(child).compute();
                    if (childNode != null) node.addChild(childNode);
                }
            } else {
                // Large directory: fork a subtask per child
                List<ScanTask> tasks = new ArrayList<>(children.size());
                for (Path child : children) {
                    tasks.add(new ScanTask(child));
                }
                // Fork all but the last, compute the last inline
                for (int i = 0; i < tasks.size() - 1; i++) {
                    tasks.get(i).fork();
                }
                FileNode last = tasks.get(tasks.size() - 1).compute();
                if (last != null) node.addChild(last);

                for (int i = 0; i < tasks.size() - 1; i++) {
                    FileNode child = tasks.get(i).join();
                    if (child != null) node.addChild(child);
                }
            }

            long count = scannedCount.incrementAndGet();
            if (count % 200 == 0) updateMessage("スキャン中: " + count + " エントリ処理済み");
            return node;
        }

        private List<Path> listChildren(Path dir) {
            List<Path> result = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path child : stream) {
                    if (!Files.isSymbolicLink(child)) result.add(child);
                }
            } catch (IOException e) {
                // skip inaccessible directory
            }
            return result;
        }
    }
}
