package com.fwmotion.threescale.cms.cli.support;

import com.fwmotion.threescale.cms.model.*;
import io.quarkus.logging.Log;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.function.Function;

@ApplicationScoped
public class CmsObjectPathKeyGenerator {

    public static final String LAYOUT_FILENAME_PREFIX = "l_";
    public static final String PARTIAL_FILENAME_PREFIX = "_";

    private static final String DEFAULT_HANDLER_SUFFIX = "";
    private static final String DEFAULT_BUILTIN_PAGE_SUFFIX = ".html";
    private static final String LIQUID_SUFFIX = ".liquid";

    private static final Map<String, String> CONTENT_TYPE_TO_FILE_EXT =
        Map.of(
            "text/css", ".css",
            "text/html", ".html",
            "text/javascript", ".js",
            "text/plain", ".txt"
        );
    private static final Map<Class<? extends CmsObject>, Function<? super CmsObject, String>> PATH_KEY_FUNCTIONS =
        Map.of(
            CmsSection.class, cmsSection -> generateKeyFromCms((CmsSection) cmsSection),
            CmsFile.class, cmsFile -> generateKeyFromCms((CmsFile) cmsFile),
            CmsBuiltinPage.class, cmsBuiltinPage -> generateKeyFromCms((CmsBuiltinPage) cmsBuiltinPage),
            CmsBuiltinPartial.class, cmsBuiltinPartial -> generateKeyFromCmsSystemName(((CmsBuiltinPartial) cmsBuiltinPartial).getSystemName(), PARTIAL_FILENAME_PREFIX),
            CmsLayout.class, cmsLayout -> generateKeyFromCmsSystemName(((CmsLayout) cmsLayout).getSystemName(), LAYOUT_FILENAME_PREFIX),
            CmsPage.class, cmsPage -> generateKeyFromCms((CmsPage) cmsPage),
            CmsPartial.class, cmsPartial -> generateKeyFromCmsSystemName(((CmsPartial) cmsPartial).getSystemName(), PARTIAL_FILENAME_PREFIX)
        );

    @Nonnull
    private static String generateKeyFromCms(@Nonnull CmsSection cmsSection) {
        return StringUtils.appendIfMissing(cmsSection.getPath(), "/");
    }

    @Nonnull
    private static String generateKeyFromCms(@Nonnull CmsFile cmsFile) {
        return cmsFile.getPath();
    }

    @Nonnull
    private static String generateKeyFromCms(@Nonnull CmsBuiltinPage cmsBuiltinPage) {
        StringBuilder pathBuilder = new StringBuilder();

        String origPath = cmsBuiltinPage.getPath();
        if (StringUtils.isEmpty(origPath)) {
            origPath = cmsBuiltinPage.getSystemName();
            pathBuilder.append('/')
                .append(cmsBuiltinPage.getSystemName());
        }

        if (StringUtils.endsWith(origPath, "/")) {
            pathBuilder.append("index");
        }

        pathBuilder.append(DEFAULT_BUILTIN_PAGE_SUFFIX);

        if (StringUtils.isNotBlank(cmsBuiltinPage.getHandler())) {
            pathBuilder.append('.').append(cmsBuiltinPage.getHandler());
        } else {
            pathBuilder.append(DEFAULT_HANDLER_SUFFIX);
        }

        return pathBuilder
            .append(LIQUID_SUFFIX)
            .toString();
    }

    @Nonnull
    private static String generateKeyFromCmsSystemName(@Nullable String systemName,
                                                       @Nonnull String filenamePrefix) {
        StringBuilder pathBuilder = new StringBuilder();

        String[] pathSections = StringUtils.split(StringUtils.trimToEmpty(systemName), '/');

        if (pathSections.length < 1) {
            throw new IllegalStateException("Unknown how to handle object with unparseable system name: " + systemName);
        }

        for (int i = 0; i < pathSections.length - 1; i++) {
            pathBuilder.append('/')
                .append(pathSections[i]);
        }

        pathBuilder.append("/")
            .append(filenamePrefix)
            .append(pathSections[pathSections.length - 1])
            .append(".html")
            .append(DEFAULT_HANDLER_SUFFIX)
            .append(LIQUID_SUFFIX);

        return pathBuilder.toString();
    }

    @Nonnull
    private static String generateKeyFromCms(@Nonnull CmsPage cmsPage) {
        StringBuilder pathBuilder = new StringBuilder(cmsPage.getPath());

        if (StringUtils.endsWith(cmsPage.getPath(), "/")) {
            pathBuilder.append("index");
        }

        String fileExt = CONTENT_TYPE_TO_FILE_EXT.get(StringUtils.trimToEmpty(StringUtils.lowerCase(cmsPage.getContentType())));
        if (fileExt != null) {
            if (!StringUtils.endsWith(cmsPage.getPath(), fileExt)) {
                pathBuilder.append(fileExt);
            }
        } else {
            Log.warn("Unknown file extension for content-type \"" + cmsPage.getContentType() + "\"");
        }

        if (StringUtils.isNotBlank(cmsPage.getHandler())) {
            pathBuilder.append('.').append(cmsPage.getHandler());
        } else {
            pathBuilder.append(DEFAULT_HANDLER_SUFFIX);
        }

        if (Boolean.TRUE == cmsPage.getLiquidEnabled()) {
            pathBuilder.append(LIQUID_SUFFIX);
        }

        return pathBuilder.toString();
    }

    @Nonnull
    public String generatePathKeyForObject(@Nonnull CmsObject cmsObject) {
        Function<? super CmsObject, String> pathKeyFunc = PATH_KEY_FUNCTIONS.get(cmsObject.getClass());

        if (pathKeyFunc == null) {
            throw new IllegalStateException("Unable to map from type to path key: " + cmsObject.getClass());
        }

        String pathKey = pathKeyFunc.apply(cmsObject);

        if (StringUtils.isBlank(pathKey)) {
            throw new IllegalStateException("Path key generator failed for " + cmsObject);
        }

        return pathKey;
    }

}
