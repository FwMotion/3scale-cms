package com.fwmotion.threescale.cms.cli.support;

import com.fwmotion.threescale.cms.model.CmsObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class LocalFileVisitor extends SimpleFileVisitor<Path> {

    private static final String CMSIGNORE_FILENAME = ".cmsignore";


    private final LocalFileCmsObjectGenerator localFileCmsObjectGenerator;
    private final CmsObjectPathKeyGenerator pathKeyGenerator;
    private final Path rootPath;
    private final Set<String> implicitSectionPaths;
    private final Consumer<String> ignorePath;
    private final BiConsumer<String, Pair<CmsObject, File>> registerCmsObjectForPath;


    private final FileSystem defaultFileSystem = FileSystems.getDefault();
    private final Deque<List<Pair<PathMatcher, Boolean>>> ignoreRules = new ArrayDeque<>();
    private int emptyDirDepth = 0;

    public LocalFileVisitor(@Nonnull LocalFileCmsObjectGenerator localFileCmsObjectGenerator,
                            @Nonnull CmsObjectPathKeyGenerator pathKeyGenerator,
                            @Nonnull Path rootPath,
                            @Nonnull Set<String> implicitSectionPaths,
                            @Nonnull Consumer<String> ignorePath,
                            @Nonnull BiConsumer<String, Pair<CmsObject, File>> registerCmsObjectForPath) {
        this.localFileCmsObjectGenerator = localFileCmsObjectGenerator;
        this.pathKeyGenerator = pathKeyGenerator;
        this.rootPath = rootPath;
        this.implicitSectionPaths = implicitSectionPaths;
        this.ignorePath = ignorePath;
        this.registerCmsObjectForPath = registerCmsObjectForPath;
    }

    @Nonnull
    @Override
    public FileVisitResult preVisitDirectory(@Nonnull Path dir,
                                             @Nonnull BasicFileAttributes attrs) throws IOException {

        // Test if the entire directory is ignored
        if (testPathIsIgnored(dir)) {
            ignorePath.accept(pathKeyFromPath(dir, true));
            return FileVisitResult.SKIP_SUBTREE;
        }

        // If directory is not ignored, try loading .cmsignore
        Path cmsIgnorePath = dir.resolve(CMSIGNORE_FILENAME);
        if (cmsIgnorePath.toFile().exists()
            && cmsIgnorePath.toFile().canRead()) {
            readCmsIgnoreFile(cmsIgnorePath);
        } else {
            ignoreRules.addLast(Collections.emptyList());
        }

        emptyDirDepth++;

        return FileVisitResult.CONTINUE;
    }

    private void generateAndRegisterObject(@Nonnull String pathKey,
                                           @Nonnull File file) {
        // Generate object first
        CmsObject cmsObject = localFileCmsObjectGenerator.generateObjectFromFile(pathKey, file);

        // Regenerate the path key; objects may have better canonical paths than
        // what is on the file system
        pathKey = pathKeyGenerator.generatePathKeyForObject(cmsObject);

        // Send the pathKey, object, and file for inclusion
        registerCmsObjectForPath.accept(pathKey, Pair.of(cmsObject, file));
    }

    @Nonnull
    @Override
    public FileVisitResult visitFile(@Nonnull Path path,
                                     @Nonnull BasicFileAttributes attrs) {

        String pathKey = pathKeyFromPath(path, false);

        if (testPathIsIgnored(path)) {
            ignorePath.accept(pathKey);
            return FileVisitResult.CONTINUE;
        }

        emptyDirDepth = 0;

        generateAndRegisterObject(pathKey, path.toFile());

        return FileVisitResult.CONTINUE;
    }

    @Nonnull
    @Override
    public FileVisitResult postVisitDirectory(@Nonnull Path dir,
                                              @Nullable IOException exc) throws IOException {
        if (exc != null) {
            throw exc;
        }

        if (!testPathIsIgnored(dir)) {
            ignoreRules.removeLast();
        }

        emptyDirDepth = Math.max(0, emptyDirDepth);

        if (emptyDirDepth == 0) {
            String pathKey = pathKeyFromPath(dir, true);

            if (!implicitSectionPaths.contains(pathKey)) {
                generateAndRegisterObject(pathKey, dir.toFile());
            }
        } else {
            emptyDirDepth--;
        }

        return FileVisitResult.CONTINUE;
    }

    private boolean testPathIsIgnored(@Nonnull Path path) {
        Path normalizedPath = path.normalize();

        List<Boolean> matchingRules = ignoreRules.stream()
            .sequential()
            .flatMap(Collection::stream)
            .filter(matcherEntry -> matcherEntry.getLeft().matches(normalizedPath))
            .map(Pair::getRight)
            .collect(Collectors.toList());

        if (matchingRules.isEmpty()) {
            // Nothing matched, so not ignored
            return false;
        }

        // Otherwise, the last item to match will determine whether
        // this path is ignored
        return matchingRules.get(matchingRules.size() - 1);
    }

    @Nonnull
    private String pathKeyFromPath(@Nonnull Path path, boolean isDirectory) {
        if (rootPath.equals(path)) {
            return "/";
        }

        Path relativePath = rootPath.relativize(path);

        return "/"
            + StreamSupport.stream(relativePath.spliterator(), false)
            .map(Path::toString)
            .collect(Collectors.joining("/"))
            + (isDirectory ? "/" : "");
    }

    private void readCmsIgnoreFile(@Nonnull Path cmsIgnorePath) throws IOException {
        try (Stream<String> lines = Files.lines(cmsIgnorePath)) {
            List<Pair<PathMatcher, Boolean>> ignoreMatchers = new ArrayList<>();

            lines.map(line -> {
                    String[] lineSectionsArray = StringUtils.splitPreserveAllTokens(line, "#", 2);

                    if (lineSectionsArray.length < 1) {
                        return "";
                    }

                    return lineSectionsArray[0];
                })
                .filter(StringUtils::isNotBlank)
                .forEach(line -> {
                    boolean isIgnored = !StringUtils.startsWith(line, "!");
                    String computedLine = StringUtils.removeStart(line, "!");

                    if (StringUtils.startsWith(computedLine, "/")
                        || StringUtils.startsWith(computedLine, defaultFileSystem.getSeparator())) {
                        ignoreMatchers.add(
                            Pair.of(
                                generatePathMatcher("." + computedLine),
                                isIgnored));
                    } else {
                        computedLine = StringUtils.removeStart(computedLine, "**/");
                        ignoreMatchers.add(
                            Pair.of(
                                generatePathMatcher("./" + computedLine),
                                isIgnored));
                        ignoreMatchers.add(
                            Pair.of(
                                generatePathMatcher("**/" + computedLine),
                                isIgnored));
                    }
                });

            ignoreRules.addLast(ignoreMatchers);
        }
    }

    private PathMatcher generatePathMatcher(String path) {
        return defaultFileSystem.getPathMatcher("glob:" + rootPath.resolve(path).normalize());
    }
}
