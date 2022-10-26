package com.fwmotion.threescale.cms.cli.support;

import com.fwmotion.threescale.cms.model.*;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ApplicationScoped
public class LocalFileCmsObjectGenerator {

    private static final Map<String, String> FILE_EXT_TO_CONTENT_TYPE =
        Map.of(
            ".css", "text/css",
            ".gif", "image/gif",
            ".htm", "text/html",
            ".html", "text/html",
            ".ico", "image/x-icon",
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".js", "text/javascript",
            ".png", "image/png",
            ".txt", "text/plain"
        );

    private static final Set<String> TEMPLATE_CONTENT_TYPES = Set.of(
        "text/css",
        "text/html",
        "text/javascript",
        "text/plain"
    );

    private static final Pattern LAYOUT_PREFIX_PATTERN = Pattern.compile("(?<path>.*/)" + CmsObjectPathKeyGenerator.LAYOUT_FILENAME_PREFIX + "(?<layoutname>[^/]+)");
    private static final Predicate<String> LAYOUT_PREFIX_PREDICATE = LAYOUT_PREFIX_PATTERN.asMatchPredicate();

    private static final Pattern PARTIAL_PREFIX_PATTERN = Pattern.compile("(?<path>.*/)" + CmsObjectPathKeyGenerator.PARTIAL_FILENAME_PREFIX + "(?<partialname>[^/]+)");
    private static final Predicate<String> PARTIAL_PREFIX_PREDICATE = PARTIAL_PREFIX_PATTERN.asMatchPredicate();

    private static final Pattern FILE_SUFFIX_PATTERN = Pattern.compile(
        "(?<filename>.*?)" +
            "(?<fileext>" +
            FILE_EXT_TO_CONTENT_TYPE.keySet().stream().map(ext -> "\\" + ext).collect(Collectors.joining("|"))
            + ")?" +
            "(?:\\.(?<handler>markdown|textile))?" +
            "(?<liquid>\\.liquid)?");

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    @Nonnull
    private static OffsetDateTime calculateUpdatedAt(@Nonnull File file) {
        return Instant.ofEpochMilli(file.lastModified()).atOffset(ZoneOffset.UTC);
    }

    @Nonnull
    public CmsObject generateObjectFromFile(@Nonnull String relativePath,
                                            @Nonnull File file) {
        if (file.isDirectory()) {
            return generateSectionFromFile(relativePath, file);
        }

        if (LAYOUT_PREFIX_PREDICATE.test(relativePath)
            || StringUtils.startsWith(relativePath, "/layouts/")) {
            return generateLayoutFromFile(relativePath, file);
        }

        if (PARTIAL_PREFIX_PREDICATE.test(relativePath)) {
            return generatePartialFromFile(relativePath, file);
        }

        Matcher matcher = FILE_SUFFIX_PATTERN.matcher(relativePath);
        if (matcher.matches()) {
            String fileExt = StringUtils.trimToEmpty(matcher.group("fileext"));
            String contentType = FILE_EXT_TO_CONTENT_TYPE.getOrDefault(fileExt, DEFAULT_CONTENT_TYPE);

            if (TEMPLATE_CONTENT_TYPES.contains(contentType)) {
                return generatePageFromFile(relativePath, file);
            }
        }

        return generateCmsFileFromFile(relativePath, file);
    }

    private void applyFileSuffixInfo(@Nonnull String path,
                                     @Nonnull Consumer<String> setContentType,
                                     @Nonnull Consumer<String> setHandler,
                                     @Nonnull Consumer<Boolean> setLiquidEnabled,
                                     @Nonnull Consumer<String> setPath) {
        Matcher matcher = FILE_SUFFIX_PATTERN.matcher(path);
        if (matcher.matches()) {
            String fileExt = StringUtils.trimToEmpty(matcher.group("fileext"));
            setContentType.accept(FILE_EXT_TO_CONTENT_TYPE.getOrDefault(fileExt, DEFAULT_CONTENT_TYPE));

            String handler = StringUtils.trimToEmpty(matcher.group("handler"));
            setHandler.accept(handler);

            if (StringUtils.isNotBlank(matcher.group("liquid"))) {
                setLiquidEnabled.accept(true);
            }

            setPath.accept(matcher.group("filename"));
        } else {
            setPath.accept(path);
        }
    }

    @Nonnull
    private CmsSection generateSectionFromFile(@Nonnull String relativePath,
                                               @Nonnull File file) {
        CmsSection section = new CmsSection();

        section.setPath(relativePath.replaceAll("/+$", ""));
        section.setPublic(true);
        section.setUpdatedAt(calculateUpdatedAt(file));

        return section;
    }

    @Nonnull
    private CmsLayout generateLayoutFromFile(@Nonnull String relativePath,
                                             @Nonnull File file) {
        CmsLayout layout = new CmsLayout();

        String transformedPath;

        Matcher matcher = LAYOUT_PREFIX_PATTERN.matcher(relativePath);
        if (matcher.matches()) {
            transformedPath = matcher.group("path") + matcher.group("layoutname");
        } else {
            transformedPath = relativePath;
        }

        applyFileSuffixInfo(transformedPath,
            layout::setContentType,
            layout::setHandler,
            layout::setLiquidEnabled,
            path -> layout.setSystemName(path.replaceFirst("^/+", "")
                .replaceFirst("^/layouts/", "/")));

        layout.setUpdatedAt(calculateUpdatedAt(file));

        return layout;
    }

    @Nonnull
    private CmsPartial generatePartialFromFile(@Nonnull String relativePath,
                                               @Nonnull File file) {
        CmsPartial partial = new CmsPartial();

        String transformedPath;

        Matcher matcher = PARTIAL_PREFIX_PATTERN.matcher(relativePath);
        if (matcher.matches()) {
            transformedPath = matcher.group("path") + matcher.group("partialname");
        } else {
            transformedPath = relativePath;
        }

        applyFileSuffixInfo(transformedPath,
            partial::setContentType,
            partial::setHandler,
            partial::setLiquidEnabled,
            path -> partial.setSystemName(path.replaceFirst("^/+", "")));

        partial.setUpdatedAt(calculateUpdatedAt(file));

        return partial;
    }

    @Nonnull
    private CmsPage generatePageFromFile(@Nonnull String relativePath,
                                         @Nonnull File file) {
        CmsPage page = new CmsPage();

        applyFileSuffixInfo(relativePath,
            page::setContentType,
            page::setHandler,
            page::setLiquidEnabled,
            path -> page.setPath(path.replaceFirst("/index$", "/")));

        page.setUpdatedAt(calculateUpdatedAt(file));

        return page;
    }

    @Nonnull
    private CmsFile generateCmsFileFromFile(@Nonnull String relativePath,
                                            @Nonnull File file) {
        CmsFile cmsFile = new CmsFile();

        cmsFile.setPath(relativePath);
        cmsFile.setUpdatedAt(calculateUpdatedAt(file));

        return cmsFile;
    }

}
