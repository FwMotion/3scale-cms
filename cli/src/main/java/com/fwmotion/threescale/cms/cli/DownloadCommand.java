package com.fwmotion.threescale.cms.cli;

import com.fwmotion.threescale.cms.cli.support.LocalRemoteObjectTreeComparator;
import com.fwmotion.threescale.cms.cli.support.LocalRemoteTreeComparisonDetails;
import com.fwmotion.threescale.cms.cli.support.PathRecursionSupport;
import com.fwmotion.threescale.cms.model.CmsFile;
import com.fwmotion.threescale.cms.model.CmsObject;
import com.fwmotion.threescale.cms.model.CmsSection;
import com.fwmotion.threescale.cms.model.CmsTemplate;
import io.quarkus.logging.Log;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;

@CommandLine.Command(
    header = "Download 3scale CMS Content",
    name = "download",
    description = "Download specified contents of the 3scale CMS to the local " +
        "folder, or all contents when no paths are supplied"
)
public class DownloadCommand extends CommandBase implements Callable<Integer> {


    @Inject
    LocalRemoteObjectTreeComparator localRemoteObjectTreeComparator;

    @Inject
    PathRecursionSupport pathRecursionSupport;

    @CommandLine.ArgGroup
    MutuallyExclusiveGroup exclusiveOptions;

    @CommandLine.ParentCommand
    private TopLevelCommand topLevelCommand;

    @CommandLine.Option(
        names = "--draft",
        description = "Download draft version of templates (otherwise, " +
            "published will be downloaded"
    )
    private boolean downloadDraft;

    @CommandLine.Option(
        names = {"-n", "--dry-run"},
        description = "Dry run: do not download any files; instead, just list " +
            "operations that would be performed"
    )
    private boolean noop;

    @Override
    public Integer call() throws Exception {

        LocalRemoteTreeComparisonDetails treeDetails =
            localRemoteObjectTreeComparator.compareLocalAndRemoteCmsObjectTrees(
                topLevelCommand.getCmsObjects().stream(),
                topLevelCommand.getRootDirectory(),
                true);

        Map<String, CmsObject> remoteObjectsByPath = treeDetails.getRemoteObjectsByCmsPath();
        Path rootPath = topLevelCommand.getRootDirectory().toPath();

        Set<String> localPathsToDelete;
        Set<String> remotePathsToDownload;

        if (isDownloadAll()) {
            if (isIncludeUnchanged()) {
                remotePathsToDownload = treeDetails.getRemoteObjectsByCmsPath().keySet();
            } else {
                remotePathsToDownload = SetUtils.union(
                    treeDetails.getRemotePathsMissingInLocal(),
                    treeDetails.getRemoteObjectsNewerThanLocal()
                );
            }

            if (isDeleteMissing()) {
                localPathsToDelete = treeDetails.getLocalPathsMissingInRemote();
            } else {
                localPathsToDelete = Collections.emptySet();
            }
        } else {
            remotePathsToDownload = pathRecursionSupport.calculateSpecifiedPaths(
                exclusiveOptions.individualFilesDownloadGroup.downloadPaths,
                recursionStyle(),
                remoteObjectsByPath);

            localPathsToDelete = Collections.emptySet();
        }

        if (localPathsToDelete.isEmpty() && remotePathsToDownload.isEmpty()) {
            Log.info("Nothing to do.");
            return 0;
        }

        List<File> deleteFiles = localPathsToDelete.stream()
            .map(treeDetails.getLocalObjectsByCmsPath()::get)
            .map(Pair::getRight)
            .toList();

        List<Pair<CmsObject, Path>> remoteObjectsToDownload = remotePathsToDownload.stream()
            .map(pathKey -> {
                CmsObject object = remoteObjectsByPath.get(pathKey);
                Path targetPath = rootPath.resolve("." + pathKey).normalize();

                return Pair.of(object, targetPath);
            })
            .toList();

        if (noop) {
            for (File file : deleteFiles) {
                Log.info("Would delete " + file);
            }

            for (Pair<CmsObject, Path> pair : remoteObjectsToDownload) {
                CmsObject cmsObject = pair.getLeft();
                Path targetPath = pair.getRight();

                String draftIndicator;
                if (cmsObject instanceof CmsTemplate) {
                    draftIndicator = downloadDraft
                        ? " (draft)"
                        : " (published)";
                } else {
                    draftIndicator = "";
                }

                Log.info("Would download " + targetPath.toString() + draftIndicator);
            }
        } else {
            for (File file : deleteFiles) {
                Log.info("Deleting " + file + "...");
                if (!file.delete()) {
                    Log.warn("Failed to delete " + file);
                }
            }
            for (Pair<CmsObject, Path> pair : remoteObjectsToDownload) {
                performDownload(pair.getLeft(), pair.getRight());
            }
        }

        return 0;
    }

    private void performDownload(@Nonnull CmsObject cmsObject,
                                 @Nonnull Path targetPath) throws IOException {
        String draftIndicator;
        if (cmsObject instanceof CmsTemplate) {
            draftIndicator = downloadDraft
                ? " (draft)"
                : " (published)";
        } else {
            draftIndicator = "";
        }

        Log.info("Downloading " + targetPath + draftIndicator);

        File targetFile = targetPath.toFile();
        File parentDirectory = targetFile.getParentFile();

        if (parentDirectory != null) {
            if (!parentDirectory.exists() && !parentDirectory.mkdirs()) {
                throw new IllegalStateException("Unable to create parent directories for " + targetFile);
            }
        }

        switch (cmsObject.getType()) {
            case SECTION:
                performDownloadSection((CmsSection) cmsObject, targetFile);
                break;
            case FILE:
                performDownloadFile((CmsFile) cmsObject, targetFile);
                break;
            case TEMPLATE:
                assert cmsObject instanceof CmsTemplate;
                performDownloadTemplate((CmsTemplate) cmsObject, targetFile);
                break;
            default:
                throw new UnsupportedOperationException("Unknown CMS type: " + cmsObject.getType());
        }

        if (!targetFile.setLastModified(cmsObject.getUpdatedAt().toInstant().toEpochMilli())) {
            Log.warn("Couldn't set last-modified for " + targetFile);
        }

    }

    private void performDownloadSection(@Nonnull CmsSection cmsSection,
                                        @Nonnull File targetFile) {
        if (!targetFile.exists() && !targetFile.mkdir()) {
            throw new IllegalStateException("Failed to create directory " + targetFile);
        }
    }

    private void performDownloadFile(@Nonnull CmsFile cmsFile,
                                     @Nonnull File targetFile) throws IOException {
        InputStream fileContent = topLevelCommand.getClient()
            .getFileContent(cmsFile)
            .orElseThrow(() -> new IllegalStateException("Failed to download file content for " + targetFile));

        FileUtils.copyInputStreamToFile(fileContent, targetFile);
    }

    private void performDownloadTemplate(@Nonnull CmsTemplate cmsTemplate,
                                         @Nonnull File targetFile) throws IOException {
        Optional<InputStream> fileContentOptional;

        if (downloadDraft) {
            fileContentOptional = topLevelCommand.getClient()
                .getTemplateDraft(cmsTemplate);
        } else {
            fileContentOptional = topLevelCommand.getClient()
                .getTemplatePublished(cmsTemplate);
        }

        InputStream fileContent = fileContentOptional
            .orElseThrow(() -> new IllegalStateException("Failed to download file content for " + targetFile));

        FileUtils.copyInputStreamToFile(fileContent, targetFile);
    }

    private boolean isDownloadAll() {
        return exclusiveOptions == null
            || exclusiveOptions.individualFilesDownloadGroup == null
            || exclusiveOptions.individualFilesDownloadGroup.downloadPaths == null
            || exclusiveOptions.individualFilesDownloadGroup.downloadPaths.isEmpty();
    }

    private boolean isIncludeUnchanged() {
        return Optional.ofNullable(exclusiveOptions)
            .map(options -> options.allFilesDownloadGroup)
            .map(allFilesDownloadGroup -> allFilesDownloadGroup.includeUnchanged)
            .orElse(false);
    }

    private boolean isDeleteMissing() {
        return Optional.ofNullable(exclusiveOptions)
            .map(options -> options.allFilesDownloadGroup)
            .map(allFilesDownloadGroup -> allFilesDownloadGroup.deleteMissing)
            .orElse(false);
    }

    private PathRecursionSupport.RecursionOption recursionStyle() {
        return Optional.ofNullable(exclusiveOptions)
            .map(options -> options.individualFilesDownloadGroup)
            .map(individualFilesDownloadGroup -> individualFilesDownloadGroup.recurseBy)
            .orElse(PathRecursionSupport.RecursionOption.PATH_PREFIX);
    }

    private static class MutuallyExclusiveGroup {

        @CommandLine.ArgGroup(exclusive = false)
        AllFilesDownloadGroup allFilesDownloadGroup;

        @CommandLine.ArgGroup(exclusive = false)
        IndividualFilesDownloadGroup individualFilesDownloadGroup;

    }

    private static class AllFilesDownloadGroup {

        @CommandLine.Option(
            names = {"-u", "--include-unchanged"},
            description = {
                "Include unchanged files in download; if not set, only " +
                    "changed and new files will be downloaded",
                "(note: only usable when downloading all CMS contents)"
            },
            defaultValue = "false"
        )
        boolean includeUnchanged;

        @CommandLine.Option(
            names = {"-d", "--delete-missing"},
            description = {
                "Delete local files that are missing from 3scale CMS",
                "(note: only usable when downloading all CMS contents)"
            },
            defaultValue = "false"
        )
        boolean deleteMissing;

    }

    private static class IndividualFilesDownloadGroup {

        @CommandLine.Option(
            names = {"-r", "--recurse-by"},
            description = {
                "Method of recursing CMS objects when a section is specified " +
                    "in PATH_KEY",
                "Options: PARENT_ID, PATH_PREFIX, NONE"
            },
            defaultValue = "PATH_PREFIX"
        )
        PathRecursionSupport.RecursionOption recurseBy;

        @CommandLine.Parameters(
            paramLabel = "PATH_KEY",
            arity = "1..*",
            description = {
                "Paths to download from 3scale CMS to local files",
                "(note: will download regardless of last-modified timestamps)"
            }
        )
        List<String> downloadPaths;

    }
}
