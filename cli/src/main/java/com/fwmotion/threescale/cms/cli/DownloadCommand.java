package com.fwmotion.threescale.cms.cli;

import com.fwmotion.threescale.cms.cli.support.LocalRemoteObjectTreeComparator;
import com.fwmotion.threescale.cms.cli.support.LocalRemoteTreeComparisonDetails;
import com.fwmotion.threescale.cms.model.*;
import io.quarkus.logging.Log;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import picocli.CommandLine;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandLine.Command(
    header = "Download 3scale CMS Content",
    name = "download",
    description = "Download specified contents of the 3scale CMS to the local " +
        "folder, or all contents when no paths are supplied"
)
public class DownloadCommand extends CommandBase implements Callable<Integer> {

    private static final Map<Class<? extends CmsObject>, Function<? super CmsObject, Integer>> GET_PARENT_ID_FUNCTIONS =
        Map.of(
            // TODO: See if there's a way to get section ID / parent ID from
            //       other object types
            CmsFile.class, file -> ((CmsFile) file).getSectionId(),
            CmsSection.class, section -> ((CmsSection) section).getParentId()
        );

    @Inject
    LocalRemoteObjectTreeComparator localRemoteObjectTreeComparator;

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
            "what operations would be performed"
    )
    private boolean noop;

    @Override
    public Integer call() throws Exception {

        LocalRemoteTreeComparisonDetails treeDetails =
            localRemoteObjectTreeComparator.compareLocalAndRemoteCmsObjectTrees(
                topLevelCommand.getCmsObjects().stream(),
                topLevelCommand.getRootDirectory()
            );

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
            List<String> downloadPaths = exclusiveOptions.individualFilesDownloadGroup.downloadPaths;

            Set<String> nonMatchingPaths = downloadPaths.stream()
                .filter(Predicate.not(treeDetails.getRemoteObjectsByCmsPath()::containsKey))
                .collect(Collectors.toSet());

            if (!nonMatchingPaths.isEmpty()) {
                throw new IllegalArgumentException("Paths do not exist in 3scale CMS: " + String.join(", ", nonMatchingPaths));
            }

            remotePathsToDownload = exclusiveOptions.individualFilesDownloadGroup.downloadPaths.stream()
                .flatMap(pathKey -> {
                    CmsObject parentObject = remoteObjectsByPath.get(pathKey);

                    RecursionOption recurseBy = recursionStyle();

                    if (recurseBy == RecursionOption.NONE
                        || parentObject.getType() != ThreescaleObjectType.SECTION) {
                        return Stream.of(pathKey);
                    }

                    if (recurseBy == RecursionOption.PATH_PREFIX) {
                        return remoteObjectsByPath.keySet().stream()
                            .filter(subKey -> StringUtils.startsWith(subKey, pathKey));
                    }

                    if (recurseBy == RecursionOption.PARENT_ID) {
                        LinkedList<Pair<String, CmsObject>> recursingList = new LinkedList<>();
                        recursingList.add(Pair.of(pathKey, parentObject));

                        ListIterator<Pair<String, CmsObject>> treeWalker = recursingList.listIterator();

                        while (treeWalker.hasNext()) {
                            Pair<String, CmsObject> currentPair = treeWalker.next();
                            CmsObject currentObject = currentPair.getRight();

                            if (currentObject.getType() != ThreescaleObjectType.SECTION) {
                                continue;
                            }

                            Integer parentId = currentObject.getId();

                            int addedChildren = Math.toIntExact(
                                remoteObjectsByPath.entrySet()
                                    .stream()
                                    .filter(childEntry -> {
                                        CmsObject childObject = childEntry.getValue();
                                        Integer childParentId = GET_PARENT_ID_FUNCTIONS.getOrDefault(childObject.getClass(), o -> Integer.MIN_VALUE)
                                            .apply(childObject);

                                        return parentId.equals(childParentId);
                                    })
                                    .peek(e -> treeWalker.add(Pair.of(e)))
                                    .count());

                            for (int i = 0; i < addedChildren; i++) {
                                treeWalker.previous();
                            }
                        }

                        return recursingList.stream()
                            .map(Pair::getKey);
                    }

                    return Stream.of(pathKey);
                })
                .collect(Collectors.toSet());
            localPathsToDelete = Collections.emptySet();
        }

        List<File> deleteFiles = localPathsToDelete.stream()
            .map(treeDetails.getLocalObjectsByCmsPath()::get)
            .map(Pair::getRight)
            .collect(Collectors.toList());

        List<Pair<CmsObject, Path>> remoteObjectsToDownload = remotePathsToDownload.stream()
            .map(pathKey -> {
                CmsObject object = remoteObjectsByPath.get(pathKey);
                Path targetPath = rootPath.resolve("." + pathKey).normalize();

                return Pair.of(object, targetPath);
            })
            .collect(Collectors.toList());

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

    private RecursionOption recursionStyle() {
        return Optional.ofNullable(exclusiveOptions)
            .map(options -> options.individualFilesDownloadGroup)
            .map(individualFilesDownloadGroup -> individualFilesDownloadGroup.recurseBy)
            .orElse(RecursionOption.PATH_PREFIX);
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

        if (!parentDirectory.exists() && !parentDirectory.mkdirs()) {
            throw new IllegalStateException("Unable to create parent directories for " + targetFile);
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

    private enum RecursionOption {
        PARENT_ID,
        PATH_PREFIX,
        NONE
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
                    "in PATH",
                "Options: PARENT_ID, PATH_PREFIX, NONE"
            },
            defaultValue = "PATH_PREFIX"
        )
        RecursionOption recurseBy;

        @CommandLine.Parameters(
            paramLabel = "PATH",
            arity = "1..*",
            description = {
                "Paths to download from 3scale CMS to local files",
                "(note: will download regardless of last-modified timestamps)"
            }
        )
        List<String> downloadPaths;

    }
}
