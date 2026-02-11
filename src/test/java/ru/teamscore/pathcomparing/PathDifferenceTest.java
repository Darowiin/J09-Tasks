package ru.teamscore.pathcomparing;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class PathDifferenceTest {
    public static final Path file1 = Paths.get("file1.txt");
    public static final Path file2 = Paths.get("file2.txt");
    public static final Path folder = Paths.get("folder");
    public static final Path anotherFile = folder.resolve("anotherFile.txt");
    public static final Path anotherDiskFile = Paths.get("D:","file.txt");

    @BeforeAll
    public static void setUpClass() throws IOException {
        List<String> lines = Arrays.asList("The first line, ", "The second line, ", "the third line, ", "the fourth line, ", "the fifth line");

        if (Files.notExists(file1)) Files.write(file1, lines, StandardCharsets.UTF_8);
        if (Files.notExists(file2)) Files.write(file2, lines.subList(0, 2), StandardCharsets.UTF_8);

        if (Files.notExists(folder)) Files.createDirectory(folder);
        if (Files.notExists(anotherFile)) Files.write(anotherFile, lines, StandardCharsets.UTF_8);

        if (Files.notExists(anotherDiskFile)) Files.write(anotherDiskFile, lines, StandardCharsets.UTF_8);
    }

    @AfterAll
    public static void tearDownClass() throws IOException {
        Files.deleteIfExists(file1);
        Files.deleteIfExists(file2);
        Files.deleteIfExists(anotherFile);
        Files.deleteIfExists(folder);
        Files.deleteIfExists(anotherDiskFile);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "file1.txt, file2.txt, BiggerFile|SameDirectory|SameAbsoluteNameDepth|SamePrefix|SameRoot",
            "file1.txt, file1.txt, SameFile|SameSizeFile|SameDirectory|SameAbsoluteNameDepth|SamePrefix|SameRoot",
            "file1.txt, folder/anotherFile.txt, SameSizeFile|SamePrefix|SameRoot",
            "folder, folder/anotherFile.txt, Subpath|SamePrefix|SameRoot",
            "file1.txt, fake.txt, NotExists"
    }, delimiter = ',')
    public void testDifference(String p1Str, String p2Str, String expectedStatusesStr) throws IOException {
        Path p1 = Paths.get(p1Str);
        Path p2 = Paths.get(p2Str);

        List<PathDifferenceStatus> expected = Arrays.stream(expectedStatusesStr.split("\\|"))
                .map(String::trim)
                .map(PathDifferenceStatus::valueOf)
                .toList();

        List<PathDifferenceStatus> actual = PathDifference.difference(p1, p2);

        Assertions.assertEquals(expected.size(), actual.size(),
                "Количество статусов не совпадает. Получено: " + actual);
        Assertions.assertTrue(actual.containsAll(expected) && expected.containsAll(actual),
                "Списки статусов не совпадают.\nОжидалось: " + expected + "\nПолучено:   " + actual);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "file1.txt, file2.txt, false",
            "file1.txt, fake.txt, true",
            "fake1.txt, fake2.txt, true"
    })
    public void testIsNotExists(Path path1, Path path2, boolean result) {
        Assertions.assertEquals(result, PathDifference.isNotExists(path1, path2));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "file1.txt, file1.txt, true",
            "file1.txt, file2.txt, false",
            "file1.txt, fake.txt, false"
    })
    public void testIsSameFile(Path path1, Path path2, boolean result) throws IOException {
        Assertions.assertEquals(result, PathDifference.isSameFile(path1, path2));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "file1.txt, file2.txt, true",
            "file2.txt, file1.txt, false",
            "file1.txt, file1.txt, false"
    })
    public void testIsBiggerFile(Path path1, Path path2, boolean result) throws IOException {
        Assertions.assertEquals(result, PathDifference.isBiggerFile(path1, path2));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "file2.txt, file1.txt, true",
            "file1.txt, file2.txt, false"
    })
    public void testIsSmallerFile(Path path1, Path path2, boolean result) throws IOException {
        Assertions.assertEquals(result, PathDifference.isSmallerFile(path1, path2));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "file1.txt, folder/anotherFile.txt, true",
            "file1.txt, file2.txt, false"
    })
    public void testIsSameSizeFile(Path path1, Path path2, boolean result) throws IOException {
        Assertions.assertEquals(result, PathDifference.isSameSizeFile(path1, path2));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "file1.txt, file2.txt, true",
            "file1.txt, folder/anotherFile.txt, false",
            "folder/anotherFile.txt, folder/anotherFile.txt, true"
    })
    public void testIsSameDirectory(Path path1, Path path2, boolean result) {
        Assertions.assertEquals(result, PathDifference.isSameDirectory(path1, path2));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "file1.txt, file2.txt, true",
            "file1.txt, folder/anotherFile.txt, false"
    })
    public void testIsSameAbsoluteNameDepth(Path path1, Path path2, boolean result) {
        Assertions.assertEquals(result, PathDifference.isSameAbsoluteNameDepth(path1, path2));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "file1.txt, file2.txt, true",
            "file1.txt, folder/anotherFile.txt, true",
            "file1.txt, D:/file.txt, false"
    })
    public void testIsSamePrefix(Path path1, Path path2, boolean result) {
        Assertions.assertEquals(result, PathDifference.isSamePrefix(path1, path2));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "file1.txt, file2.txt, true",
            "file1.txt, folder/anotherFile.txt, true",
            "file1.txt, D:/file.txt, false",
            "C:/Windows/System32, D:/file, false"
    })
    public void testIsSameRoot(Path path1, Path path2, boolean result) {
        Assertions.assertEquals(result, PathDifference.isSameRoot(path1, path2));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "folder, folder/anotherFile.txt, true",
            "folder/anotherFile.txt, folder, false",
            "file1.txt, folder/anotherFile.txt, false",
            "folder, file1.txt, false"
    })
    public void testIsSubpath(Path path1, Path path2, boolean result) {
        Assertions.assertEquals(result, PathDifference.isSubpath(path1, path2));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "folder/anotherFile.txt, folder, true",
            "folder, folder/anotherFile.txt, false",
            "file1.txt, folder, false"
    })
    public void testIsParentPath(Path path1, Path path2, boolean result) {
        Assertions.assertEquals(result, PathDifference.isParentPath(path1, path2));
    }
}
