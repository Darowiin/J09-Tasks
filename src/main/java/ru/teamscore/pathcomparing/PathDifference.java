package ru.teamscore.pathcomparing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Утилитный класс для сравнения двух путей в файловой системе.
 * Позволяет находить различные логические и физические совпадения между путями.
 */
public class PathDifference {

    /**
     * Сравнивает два пути и возвращает список всех применимых к ним статусов.
     *
     * @param path1 первый путь для сравнения
     * @param path2 второй путь для сравнения
     * @return список статусов {@link PathDifferenceStatus}, описывающих совпадения и отличия
     * @throws IOException если возникает ошибка доступа к файловой системе при проверке свойств файлов
     */
    public static List<PathDifferenceStatus> difference(Path path1, Path path2) throws IOException {
        List<PathDifferenceStatus> differences = new ArrayList<>();

        if (isNotExists(path1, path2)) {
            differences.add(PathDifferenceStatus.NotExists);
            return differences;
        }

        if (isSameFile(path1, path2)) {
            differences.add(PathDifferenceStatus.SameFile);
        }

        if (isSameDirectory(path1, path2)) {
            differences.add(PathDifferenceStatus.SameDirectory);
        }

        if (Files.isRegularFile(path1) && Files.isRegularFile(path2)) {
            if (isBiggerFile(path1, path2)) {
                differences.add(PathDifferenceStatus.BiggerFile);
            } else if (isSmallerFile(path1, path2)) {
                differences.add(PathDifferenceStatus.SmallerFile);
            } else {
                differences.add(PathDifferenceStatus.SameSizeFile);
            }
        }

        if (isSameAbsoluteNameDepth(path1, path2)) {
            differences.add(PathDifferenceStatus.SameAbsoluteNameDepth);
        }
        if (isSamePrefix(path1, path2)) {
            differences.add(PathDifferenceStatus.SamePrefix);
        }
        if (isSameRoot(path1, path2)) {
            differences.add(PathDifferenceStatus.SameRoot);
        }

        if (isSubpath(path1, path2)) {
            differences.add(PathDifferenceStatus.Subpath);
        } else if (isParentPath(path1, path2)) {
            differences.add(PathDifferenceStatus.ParentPath);
        }

        return differences;
    }

    /**
     * Проверяет, что хотя бы один из путей не существует.
     *
     * @param path1 первый путь
     * @param path2 второй путь
     * @return true, если хотя бы один путь не существует на диске
     */
    public static boolean isNotExists(Path path1, Path path2) {
        return Files.notExists(path1) || Files.notExists(path2);
    }

    /**
     * Проверяет, ведут ли оба пути к одному и тому же физическому файлу или директории.
     *
     * @param path1 первый путь
     * @param path2 второй путь
     * @return true, если пути указывают на один и тот же существующий файл
     * @throws IOException при ошибке ввода-вывода
     */
    public static boolean isSameFile(Path path1, Path path2) throws IOException {
        return !isNotExists(path1, path2) && Files.isSameFile(path1, path2);
    }

    /**
     * Проверяет, является ли первый файл строго больше второго.
     *
     * @param path1 первый путь
     * @param path2 второй путь
     * @return true, если размер первого файла больше второго
     * @throws IOException при ошибке доступа к файлу
     */
    public static boolean isBiggerFile(Path path1, Path path2) throws IOException {
        return !isNotExists(path1, path2) && Files.size(path1) > Files.size(path2);
    }

    /**
     * Проверяет, является ли первый файл строго меньше второго.
     *
     * @param path1 первый путь
     * @param path2 второй путь
     * @return true, если размер первого файла меньше второго
     * @throws IOException при ошибке доступа к файлу
     */
    public static boolean isSmallerFile(Path path1, Path path2) throws IOException {
        return !isNotExists(path1, path2) && Files.size(path1) < Files.size(path2);
    }

    /**
     * Проверяет, совпадают ли размеры файлов.
     *
     * @param path1 первый путь
     * @param path2 второй путь
     * @return true, если размеры файлов равны
     * @throws IOException при ошибке доступа к файлу
     */
    public static boolean isSameSizeFile(Path path1, Path path2) throws IOException {
        return !isNotExists(path1, path2) && Files.size(path1) == Files.size(path2);
    }

    /**
     * Проверяет, лежат ли оба файла/директории в одной и той же родительской директории.
     *
     * @param path1 первый путь
     * @param path2 второй путь
     * @return true, если файлы находятся в одной директории
     */
    public static boolean isSameDirectory(Path path1, Path path2) {
        if (isNotExists(path1, path2)) return false;

        Path parent1 = path1.toAbsolutePath().normalize().getParent();
        Path parent2 = path2.toAbsolutePath().normalize().getParent();

        if (parent1 == null || parent2 == null) {
            return false;
        }

        try {
            return Files.isSameFile(parent1, parent2);
        } catch (IOException e) {
            return parent1.equals(parent2);
        }
    }

    /**
     * Проверяет, одинаковое ли количество элементов в абсолютных путях к файлам.
     *
     * @param path1 первый путь
     * @param path2 второй путь
     * @return true, если глубина абсолютных путей совпадает
     */
    public static boolean isSameAbsoluteNameDepth(Path path1, Path path2) {
        if (isNotExists(path1, path2)) return false;
        return path1.toAbsolutePath().getNameCount() == path2.toAbsolutePath().getNameCount();
    }

    /**
     * Проверяет, имеют ли абсолютные пути одинаковое начало (первую папку после корня).
     *
     * @param path1 первый путь
     * @param path2 второй путь
     * @return true, если префиксы путей совпадают
     */
    public static boolean isSamePrefix(Path path1, Path path2) {
        if (isNotExists(path1, path2)) return false;

        Path path1Abs = path1.toAbsolutePath();
        Path path2Abs = path2.toAbsolutePath();

        if (path1Abs.getNameCount() == 0 || path2Abs.getNameCount() == 0) {
            return false;
        }

        return path1Abs.getName(0).equals(path2Abs.getName(0));
    }

    /**
     * Проверяет, имеют ли пути общий корень (например, один диск в Windows).
     *
     * @param path1 первый путь
     * @param path2 второй путь
     * @return true, если корни совпадают
     */
    public static boolean isSameRoot(Path path1, Path path2) {
        if (isNotExists(path1, path2)) return false;

        Path root1 = path1.toAbsolutePath().getRoot();
        Path root2 = path2.toAbsolutePath().getRoot();

        if (root1 == null || root2 == null) return false;

        return root1.equals(root2);
    }

    /**
     * Проверяет, находится ли path2 строго внутри path1.
     *
     * @param path1 первый путь (предполагаемая родительская директория)
     * @param path2 второй путь (предполагаемый вложенный файл/директория)
     * @return true, если path2 является подпутем path1
     */
    public static boolean isSubpath(Path path1, Path path2) {
        if (isNotExists(path1, path2)) return false;

        Path path1Norm = path1.toAbsolutePath().normalize();
        Path path2Norm = path2.toAbsolutePath().normalize();

        return !path1Norm.equals(path2Norm) && path2Norm.startsWith(path1Norm);
    }

    /**
     * Проверяет, находится ли path1 строго внутри path2.
     *
     * @param path1 первый путь (предполагаемый вложенный файл/директория)
     * @param path2 второй путь (предполагаемая родительская директория)
     * @return true, если path1 является подпутем path2
     */
    public static boolean isParentPath(Path path1, Path path2)  {
        return isSubpath(path2, path1);
    }
}
