package ru.teamscore.nestingdepth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class DirectoryDepthTest {
    @TempDir
    Path tempDir;

    @Test
    void testPathNotExists() throws IOException {
        Path nonExistentPath = tempDir.resolve("ghost_folder");

        int result = DirectoryDepth.calculateMaxDepth(nonExistentPath);

        Assertions.assertEquals(-1, result);
    }

    @Test
    void testPathIsFile() throws IOException {
        Path file = tempDir.resolve("readme.txt");
        Files.createFile(file);

        int result = DirectoryDepth.calculateMaxDepth(file);

        Assertions.assertEquals(-1, result);
    }

    @Test
    void testEmptyDirectory() throws IOException {
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectory(emptyDir);

        int result = DirectoryDepth.calculateMaxDepth(emptyDir);

        Assertions.assertEquals(0, result);
    }

    @Test
    void testDirectoryWithFilesOnly() throws IOException {
        Path dir = tempDir.resolve("files_only");
        Files.createDirectory(dir);
        Files.createFile(dir.resolve("file1.txt"));
        Files.createFile(dir.resolve("file2.log"));

        int result = DirectoryDepth.calculateMaxDepth(dir);

        Assertions.assertEquals(0, result);
    }

    @Test
    void testDepthOne() throws IOException {
        Path root = tempDir.resolve("root");
        Files.createDirectory(root);
        Files.createDirectory(root.resolve("subDir"));

        int result = DirectoryDepth.calculateMaxDepth(root);

        Assertions.assertEquals(1, result);
    }

    @Test
    void testDeepNesting() throws IOException {
        Path root = tempDir.resolve("root_deep");
        Files.createDirectories(root.resolve("level1/level2/level3"));

        int result = DirectoryDepth.calculateMaxDepth(root);

        Assertions.assertEquals(3, result);
    }

    @Test
    void testMixedDepthBranches() throws IOException {
        Path root = tempDir.resolve("root_mixed");
        Files.createDirectories(root.resolve("branchA"));
        Files.createDirectories(root.resolve("branchB/subB"));

        int result = DirectoryDepth.calculateMaxDepth(root);

        Assertions.assertEquals(2, result);
    }

    @Test
    void testSiblingsDoNotSumUp() throws IOException {
        Path root = tempDir.resolve("siblings");
        Files.createDirectories(root.resolve("folder1"));
        Files.createDirectories(root.resolve("folder2"));
        Files.createDirectories(root.resolve("folder3"));

        int result = DirectoryDepth.calculateMaxDepth(root);

        Assertions.assertEquals(1, result);
    }
}
