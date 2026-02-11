package ru.teamscore.pathcomparing;

public enum PathDifferenceStatus {
    NotExists,
    SameFile,
    BiggerFile,
    SmallerFile,
    SameSizeFile,
    SameDirectory,
    SameAbsoluteNameDepth,
    SamePrefix,
    SameRoot,
    Subpath,
    ParentPath
}
