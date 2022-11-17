package com.fwmotion.threescale.cms.cli;

import com.fwmotion.threescale.cms.ThreescaleCmsClient;
import com.fwmotion.threescale.cms.cli.support.*;
import com.fwmotion.threescale.cms.model.*;
import com.redhat.threescale.rest.cms.ApiException;
import io.quarkus.logging.Log;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import picocli.CommandLine;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(
    header = "Upload 3scale CMS Content",
    name = "upload",
    description = "Upload all content, specified file, or all specified " +
        "folder contents to CMS"
)
public class UploadCommand extends CommandBase implements Callable<Integer> {

    @Inject
    CmsObjectPathKeyGenerator pathKeyGenerator;

    @Inject
    LocalRemoteObjectTreeComparator localRemoteObjectTreeComparator;

    @Inject
    PathRecursionSupport pathRecursionSupport;

    @Inject
    CmsSectionToTopComparator sectionToTopComparator;

    @CommandLine.ArgGroup
    MutuallyExclusiveGroup exclusiveOptions;

    @CommandLine.ParentCommand
    private TopLevelCommand topLevelCommand;

    @CommandLine.Option(
        names = {"--keep-as-draft"},
        description = "Keep the uploaded content in draft and do not publish"
    )
    private boolean keepAsDraft;

    @CommandLine.Option(
        names = {"--layout"},
        arity = "1",
        paramLabel = "layout_file_name",
        description = "Specify layout for new (not updated) pages"
    )
    private String layoutFilename;

    @CommandLine.Option(
        names = {"-n", "--dry-run"},
        description = "Dry run: do not upload any files; instead, just list " +
            "operations that would be performed"
    )
    private boolean noop;

    @Override
    public Integer call() throws Exception {
        LocalRemoteTreeComparisonDetails treeDetails =
            localRemoteObjectTreeComparator.compareLocalAndRemoteCmsObjectTrees(
                topLevelCommand.getCmsObjects().stream(),
                topLevelCommand.getRootDirectory(),
                !isDeleteMissing());

        Map<String, CmsObject> remoteObjectsByPath = treeDetails.getRemoteObjectsByCmsPath();
        Map<String, Pair<CmsObject, File>> localObjectsByPath = treeDetails.getLocalObjectsByCmsPath();

        Set<String> remotePathsToDelete;
        Set<String> localPathsToUpload;

        if (isUploadAll()) {
            if (isIncludeUnchanged()) {
                localPathsToUpload = new HashSet<>(treeDetails.getLocalObjectsByCmsPath().keySet());
            } else {
                localPathsToUpload = new HashSet<>(treeDetails.getLocalPathsMissingInRemote());
                localPathsToUpload.addAll(treeDetails.getLocalObjectsNewerThanRemote());
            }

            if (isDeleteMissing()) {
                remotePathsToDelete = treeDetails.getRemotePathsMissingInLocal();
            } else {
                remotePathsToDelete = Collections.emptySet();
            }
        } else {
            Map<String, CmsObject> simplifiedLocalObjectsByPath = localObjectsByPath.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getKey()));

            localPathsToUpload = new HashSet<>(
                pathRecursionSupport.calculateSpecifiedPaths(
                    exclusiveOptions.individualFilesUploadGroup.uploadPaths,
                    recursionStyle(),
                    simplifiedLocalObjectsByPath));

            remotePathsToDelete = Collections.emptySet();
        }

        String newPageLayoutSystemName;
        if (layoutFilename == null) {
            newPageLayoutSystemName = treeDetails.getDefaultLayout()
                .map(CmsLayout::getSystemName)
                .orElse("");
        } else if (StringUtils.isBlank(layoutFilename)) {
            newPageLayoutSystemName = "";
        } else {
            CmsObject layout = Optional.ofNullable(localObjectsByPath.get(layoutFilename))
                .map(Pair::getLeft)
                .orElseGet(() -> remoteObjectsByPath.get(layoutFilename));

            if (layout instanceof CmsLayout) {
                newPageLayoutSystemName = ((CmsLayout) layout).getSystemName();
            } else {
                throw new IllegalArgumentException("Specified layout for new pages is not a layout!");
            }
        }

        if (remotePathsToDelete.isEmpty() && localPathsToUpload.isEmpty()) {
            Log.info("Nothing to do.");
            return 0;
        }

        List<CmsObject> deleteObjects = remotePathsToDelete.stream()
            .map(remoteObjectsByPath::get)
            .sorted(sectionToTopComparator
                .reversed())
            .collect(Collectors.toList());

        Comparator<Pair<CmsObject, File>> uploadSortComparator;
        if (StringUtils.isNotBlank(newPageLayoutSystemName)) {
            uploadSortComparator = Comparator.comparing(Pair::getLeft,
                sectionToTopComparator
                    .thenComparing((o1, o2) -> {
                        if (o1 instanceof CmsLayout && StringUtils.equals(newPageLayoutSystemName, ((CmsLayout) o1).getSystemName())) {
                            return -1;
                        }

                        if (o2 instanceof CmsLayout && StringUtils.equals(newPageLayoutSystemName, ((CmsLayout) o2).getSystemName())) {
                            return 1;
                        }

                        return 0;
                    }));
        } else {
            uploadSortComparator = Comparator.comparing(Pair::getLeft, sectionToTopComparator);
        }

        List<Pair<CmsObject, File>> localObjectsToUpload = localPathsToUpload.stream()
            .map(pathKey -> {
                Pair<CmsObject, File> localObjectPair = localObjectsByPath.get(pathKey);
                CmsObject remoteObject = remoteObjectsByPath.get(pathKey);

                if (remoteObject == null) {
                    CmsObject localObject = localObjectPair.getLeft();
                    setRequiredCreationProperties(localObject, newPageLayoutSystemName);
                } else {
                    updateObjectId(localObjectPair.getLeft(), remoteObject);
                }

                return localObjectPair;
            })
            .sorted(uploadSortComparator)
            .collect(Collectors.toList());

        if (noop) {
            for (CmsObject object : deleteObjects) {
                Log.info("Would delete " + object.getType() + " " + pathKeyGenerator.generatePathKeyForObject(object));
            }

            for (Pair<CmsObject, File> pair : localObjectsToUpload) {
                CmsObject object = pair.getLeft();
                Log.info("Would upload " + object.getType() + " " + pathKeyGenerator.generatePathKeyForObject(object));
            }

            if (!keepAsDraft) {
                for (Pair<CmsObject, File> pair : localObjectsToUpload) {
                    CmsObject object = pair.getLeft();

                    if (object.getType() == ThreescaleObjectType.TEMPLATE) {
                        Log.info("Would publish " + object.getType() + " " + pathKeyGenerator.generatePathKeyForObject(object));
                    }
                }
            }

        } else {
            ThreescaleCmsClient client = topLevelCommand.getClient();

            DeleteCommand.deleteObjects(client, pathKeyGenerator, deleteObjects);

            for (Pair<CmsObject, File> pair : localObjectsToUpload) {
                performUpload(client, pair.getLeft(), pair.getRight(), localObjectsByPath, remoteObjectsByPath);
            }

            if (!keepAsDraft) {
                for (Pair<CmsObject, File> pair : localObjectsToUpload) {
                    CmsObject object = pair.getLeft();

                    if (object instanceof CmsTemplate) {
                        Log.info("Publishing " + object.getType() + " " + pathKeyGenerator.generatePathKeyForObject(object) + "...");
                        client.publish((CmsTemplate) object);
                    }
                }
            }
        }

        return 0;
    }

    private void performUpload(@Nonnull ThreescaleCmsClient client,
                               @Nonnull CmsObject object,
                               @Nonnull File file,
                               @Nonnull Map<String, Pair<CmsObject, File>> localFilesByPathKey,
                               @Nonnull Map<String, CmsObject> remoteObjectsByPathKey) throws ApiException {
        String pathKey = pathKeyGenerator.generatePathKeyForObject(object);
        Log.info("Uploading " + object.getType() + " " + pathKey + "...");

        if (object instanceof CmsSection) {
            CmsSection section = (CmsSection) object;
            if (section.getParentId() == null && !"/".equals(pathKey)) {
                section.setParentId(findParentId(pathKey, localFilesByPathKey, remoteObjectsByPathKey));
            }
            client.save(section);
        } else if (object instanceof CmsFile) {
            CmsFile cmsFile = (CmsFile) object;
            if (cmsFile.getSectionId() == null) {
                cmsFile.setSectionId(findParentId(pathKey, localFilesByPathKey, remoteObjectsByPathKey));
            }
            client.save(cmsFile, file);
        } else if (object instanceof CmsTemplate) {
            if (object instanceof CmsPage && ((CmsPage) object).getSectionId() == null) {
                ((CmsPage) object).setSectionId(findParentId(pathKey, localFilesByPathKey, remoteObjectsByPathKey));
            }
            client.save((CmsTemplate) object, file);
        } else {
            throw new UnsupportedOperationException("Unknown object type " + object.getClass());
        }
    }

    private Integer findParentId(@Nonnull String pathKey,
                                 @Nonnull Map<String, Pair<CmsObject, File>> localFilesByPathKey,
                                 @Nonnull Map<String, CmsObject> remoteObjectsByPathKey) {
        String parentPathKey;
        if (pathKey.endsWith("/")) {
            parentPathKey = pathKey.substring(0, pathKey.lastIndexOf('/', pathKey.length() - 2) + 1);
        } else {
            parentPathKey = pathKey.substring(0, pathKey.lastIndexOf('/') + 1);
        }

        do {
            CmsObject potentialParent = Optional.ofNullable(localFilesByPathKey.get(parentPathKey))
                .map(Pair::getLeft)
                .orElse(null);
            if (potentialParent != null && potentialParent.getId() != null) {
                return potentialParent.getId();
            }

            potentialParent = remoteObjectsByPathKey.get(parentPathKey);
            if (potentialParent != null) {
                return potentialParent.getId();
            }

            parentPathKey = parentPathKey.substring(0, pathKey.lastIndexOf('/', parentPathKey.length() - 2) + 1);
        } while (!"".equals(parentPathKey));

        throw new IllegalStateException("Couldn't find any parent section ID... not even root");
    }

    private void setRequiredCreationProperties(@Nonnull CmsObject localObject,
                                               @Nonnull String newPageLayoutSystemName) {
        if (localObject instanceof CmsPage) {
            CmsPage localPage = (CmsPage) localObject;

            if ("text/html".equals(localPage.getContentType())) {
                localPage.setLayout(newPageLayoutSystemName);
            } else {
                localPage.setLayout("");
            }
        }
    }

    private void updateObjectId(@Nonnull CmsObject target,
                                @Nonnull CmsObject source) {
        if (target.getType() != source.getType()) {
            throw new IllegalArgumentException("Cannot change types with update. " +
                "Source type: " + source.getType() + "; " +
                "Target type: " + target.getType());
        }

        // Only set ID... Leave the rest for 3scale to dictate what's updatable
        // and not on each type of object.

        if (target instanceof CmsSection) {
            CmsSection targetSection = (CmsSection) target;
            CmsSection sourceSection = (CmsSection) source;

            targetSection.setId(source.getId());
            targetSection.setParentId(sourceSection.getParentId());
        } else if (target instanceof CmsFile) {
            CmsFile targetFile = (CmsFile) target;
            CmsFile sourceFile = (CmsFile) source;
            targetFile.setId(sourceFile.getId());
            targetFile.setSectionId(sourceFile.getSectionId());
        } else if (target instanceof CmsLayout) {
            ((CmsLayout) target).setId(source.getId());
        } else if (target instanceof CmsPage) {
            CmsPage targetPage = (CmsPage) target;
            targetPage.setId(source.getId());

            if (source instanceof CmsPage) {
                targetPage.setSectionId(((CmsPage) source).getSectionId());
            }
        } else if (target instanceof CmsPartial) {
            ((CmsPartial) target).setId(source.getId());
        } else {
            // Unknown... built-in pages/partials shouldn't come from local
            // files anyway
            throw new UnsupportedOperationException("Unknown how to set ID for " + target.getType());
        }
    }

    private boolean isUploadAll() {
        return exclusiveOptions == null
            || exclusiveOptions.individualFilesUploadGroup == null
            || exclusiveOptions.individualFilesUploadGroup.uploadPaths == null
            || exclusiveOptions.individualFilesUploadGroup.uploadPaths.isEmpty();
    }

    private boolean isIncludeUnchanged() {
        return Optional.ofNullable(exclusiveOptions)
            .map(options -> options.allFilesUploadGroup)
            .map(allFilesDownloadGroup -> allFilesDownloadGroup.includeUnchanged)
            .orElse(false);
    }

    private boolean isDeleteMissing() {
        return Optional.ofNullable(exclusiveOptions)
            .map(options -> options.allFilesUploadGroup)
            .map(allFilesDownloadGroup -> allFilesDownloadGroup.deleteMissing)
            .orElse(false);
    }

    private PathRecursionSupport.RecursionOption recursionStyle() {
        boolean shouldRecurse = Optional.ofNullable(exclusiveOptions)
            .map(options -> options.individualFilesUploadGroup)
            .map(individualFilesDownloadGroup -> individualFilesDownloadGroup.recurseSubdirectories)
            .orElse(false);

        if (shouldRecurse) {
            return PathRecursionSupport.RecursionOption.PATH_PREFIX;
        } else {
            return PathRecursionSupport.RecursionOption.NONE;
        }
    }

    private static class MutuallyExclusiveGroup {

        @CommandLine.ArgGroup(exclusive = false)
        AllFilesUploadGroup allFilesUploadGroup;

        @CommandLine.ArgGroup(exclusive = false)
        IndividualFilesUploadGroup individualFilesUploadGroup;

    }

    private static class AllFilesUploadGroup {

        @CommandLine.Option(
            names = {"-u", "--include-unchanged"},
            description = {
                "Include unchanged files in upload; if not set, only " +
                    "changed and new files will be uploaded",
                "(note: only usable when uploading all local contents)"
            },
            defaultValue = "false"
        )
        boolean includeUnchanged;

        @CommandLine.Option(
            names = {"-d", "--delete-missing"},
            description = {
                "Delete remote objects that are missing from local filesystem",
                "(note: only usable when uploading all local contents)"
            },
            defaultValue = "false"
        )
        boolean deleteMissing;

    }

    private static class IndividualFilesUploadGroup {

        @CommandLine.Option(
            names = {"-r", "--recurse"},
            description = {
                "Flag to recurse subdirectories when uploading"
            },
            defaultValue = "false"
        )
        boolean recurseSubdirectories;

        @CommandLine.Parameters(
            paramLabel = "PATH",
            arity = "1..*",
            description = {
                "Paths to upload from local files into 3scale CMS",
                "(note: will upload regardless of last-modified timestamps)"
            }
        )
        List<String> uploadPaths;

    }
}
