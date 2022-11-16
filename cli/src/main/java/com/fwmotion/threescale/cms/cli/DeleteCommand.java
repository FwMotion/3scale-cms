package com.fwmotion.threescale.cms.cli;

import com.fwmotion.threescale.cms.ThreescaleCmsClient;
import com.fwmotion.threescale.cms.cli.support.CmsObjectPathKeyGenerator;
import com.fwmotion.threescale.cms.cli.support.CmsSectionToTopComparator;
import com.fwmotion.threescale.cms.cli.support.PathRecursionSupport;
import com.fwmotion.threescale.cms.model.CmsObject;
import io.quarkus.logging.Log;
import picocli.CommandLine;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(
    header = "Delete 3scale CMS Content",
    name = "delete",
    description = "Delete all content, specified file, or all specified " +
        "folder contents in the 3scale CMS"
)
public class DeleteCommand extends CommandBase implements Callable<Integer> {

    private static final String REALLY_DELETE_FLAG = "--yes-i-really-want-to-delete-the-entire-developer-portal";

    @Inject
    CmsObjectPathKeyGenerator pathKeyGenerator;

    @Inject
    PathRecursionSupport pathRecursionSupport;

    @Inject
    CmsSectionToTopComparator sectionToTopComparator;

    @CommandLine.ArgGroup
    MutuallyExclusiveGroup exclusiveOptions;

    @CommandLine.ParentCommand
    private TopLevelCommand topLevelCommand;
    @CommandLine.Option(
        names = {"-n", "--dry-run"},
        description = "Dry run: do not delete any files; instead, just list " +
            "operations that would be performed"
    )
    private boolean noop;

    @Nonnull
    @Override
    public Integer call() throws Exception {

        List<String> remotePathsToDelete;
        PathRecursionSupport.RecursionOption recursionStyle;

        if (isDeleteAll()) {
            if (!noop
                && !isReallyDeleteEverything()) {
                Log.error("Potentially destructive command avoided; re-run the command with option `" + REALLY_DELETE_FLAG + "` to continue anyway.");
                return 1;
            }

            remotePathsToDelete = Collections.singletonList("/");
            recursionStyle = PathRecursionSupport.RecursionOption.PATH_PREFIX;
        } else {
            remotePathsToDelete = exclusiveOptions.individualFilesDeletionGroup.paths;
            recursionStyle = recursionStyle();

            if (remotePathsToDelete.contains("/")
                && recursionStyle != PathRecursionSupport.RecursionOption.NONE) {

                Log.error("Root path specified for deletion with recursion; re-run command with no paths specified to delete all contents");
                return 1;
            }
        }

        Map<String, CmsObject> remoteObjectsByPath = topLevelCommand.getCmsObjects()
            .stream()
            .collect(Collectors.toMap(pathKeyGenerator::generatePathKeyForObject, o -> o));

        List<CmsObject> remoteObjectsToDelete = pathRecursionSupport.calculateSpecifiedPaths(
                remotePathsToDelete,
                recursionStyle,
                remoteObjectsByPath)
            .stream()
            .map(remoteObjectsByPath::get)
            .sorted(sectionToTopComparator
                .thenComparing(CmsObject::getId)
                .reversed())
            .collect(Collectors.toList());

        if (remoteObjectsToDelete.isEmpty()) {
            Log.info("Nothing to do.");
            return 0;
        }

        if (noop) {
            for (CmsObject object : remoteObjectsToDelete) {
                Log.info("Would delete " + object.getType() + " " + pathKeyGenerator.generatePathKeyForObject(object));
            }
        } else {
            ThreescaleCmsClient client = topLevelCommand.getClient();

            deleteObjects(client, pathKeyGenerator, remoteObjectsToDelete);
        }

        return 0;
    }

    static void deleteObjects(ThreescaleCmsClient client, CmsObjectPathKeyGenerator pathKeyGenerator, List<CmsObject> remoteObjectsToDelete) {
        for (CmsObject object : remoteObjectsToDelete) {
            String objectName = object.getType() + " " + pathKeyGenerator.generatePathKeyForObject(object);

            Log.info("Deleting " + objectName);
            try {
                client.delete(object);
            } catch (Exception e) {
                // TODO: Handle exceptions properly, and provide better logs
                //       indicating the reason for failure
                Log.warn("Failed to delete " + objectName, e);
            }
        }
    }

    private boolean isDeleteAll() {
        return exclusiveOptions == null
            || exclusiveOptions.individualFilesDeletionGroup == null
            || exclusiveOptions.individualFilesDeletionGroup.paths == null
            || exclusiveOptions.individualFilesDeletionGroup.paths.isEmpty();
    }

    private boolean isReallyDeleteEverything() {
        return exclusiveOptions != null
            && exclusiveOptions.allFilesDeletionGroup != null
            && exclusiveOptions.allFilesDeletionGroup.reallyDeleteEverything;
    }

    private PathRecursionSupport.RecursionOption recursionStyle() {
        return Optional.ofNullable(exclusiveOptions)
            .map(options -> options.individualFilesDeletionGroup)
            .map(individualFilesDeletionGroup -> individualFilesDeletionGroup.recurseBy)
            .orElse(PathRecursionSupport.RecursionOption.PATH_PREFIX);
    }

    private static class MutuallyExclusiveGroup {

        @CommandLine.ArgGroup(exclusive = false)
        AllFilesDeletionGroup allFilesDeletionGroup;

        @CommandLine.ArgGroup(exclusive = false)
        IndividualFilesDeletionGroup individualFilesDeletionGroup;

    }

    private static class AllFilesDeletionGroup {

        @CommandLine.Option(
            names = REALLY_DELETE_FLAG,
            hidden = true,
            defaultValue = "false"
        )
        boolean reallyDeleteEverything;

    }

    private static class IndividualFilesDeletionGroup {

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
            index = "0",
            arity = "0..*",
            paramLabel = "PATH_KEY",
            description = "Paths in developer portal to be deleted. If omitted, " +
                "all CMS objects will be deleted from the 3scale tenant."
        )
        List<String> paths;

    }

}
