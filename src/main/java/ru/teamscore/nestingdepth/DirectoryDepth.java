package ru.teamscore.nestingdepth;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class DirectoryDepth {

    /**
     * Реализация FileVisitor для подсчета максимальной глубины.
     */
    private static class MaxDepthVisitor extends SimpleFileVisitor<Path> {
        private final Path startPath;
        private int maxDepth = 0;

        public MaxDepthVisitor(Path startPath) {
            this.startPath = startPath;
        }

        public int getMaxDepth() {
            return maxDepth;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (dir.equals(startPath)) {
                return FileVisitResult.CONTINUE;
            }

            Path relativePath = startPath.relativize(dir);
            int currentDepth = relativePath.getNameCount();

            if (currentDepth > maxDepth) {
                maxDepth = currentDepth;
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            System.err.println("Access denied: " + file);
            return FileVisitResult.CONTINUE;
        }
    }

    /**
     * Метод вычисления глубины согласно условию задачи.
     * @param path путь к директории
     * @return глубина вложенности или -1, если путь не является директорией
     */
    public static int calculateMaxDepth(Path path) throws IOException {
        if (Files.notExists(path) || !Files.isDirectory(path)) {
            return -1;
        }

        MaxDepthVisitor visitor = new MaxDepthVisitor(path);
        Files.walkFileTree(path, visitor);

        return visitor.getMaxDepth();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java DirectoryDepth <path>");
            return;
        }

        String inputPath = args[0];
        Path path = Paths.get(inputPath);

        try {
            int depth = calculateMaxDepth(path);

            if (depth == -1) {
                System.out.println("This is not a directory");
            } else {
                System.out.println("Max depth: " + depth);
            }
        } catch (IOException e) {
            System.err.println("Error walking the file tree: " + e.getMessage());
        }
    }
}
