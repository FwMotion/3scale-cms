package com.fwmotion.threescale.cms.cli;

import com.fwmotion.threescale.cms.ThreescaleCmsClient;
import com.fwmotion.threescale.cms.cli.support.CmsObjectPathKeyGenerator;
import com.redhat.threescale.rest.cms.ApiException;
import io.quarkus.logging.Log;
import picocli.CommandLine;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(
    header = "Delete 3scale CMS Content",
    name = "delete",
    description = "Delete all possible content in the specified CMS"
)
public class DeleteCommand extends CommandBase implements Callable<Integer> {

    @Inject
    CmsObjectPathKeyGenerator cmsObjectPathKeyGenerator;

    @CommandLine.ParentCommand
    private TopLevelCommand topLevelCommand;

    @CommandLine.Parameters(
        index = "0",
        arity = "0..*",
        paramLabel = "PATH",
        description = "Paths in developer portal to be deleted. If omitted, " +
            "all CMS objects will be deleted from the 3scale tenant."
    )
    private List<String> paths;

    @CommandLine.Option(
        names = "--yes-i-really-want-to-delete-the-entire-developer-portal",
        hidden = true,
        defaultValue = "false"
    )
    private boolean reallyDeleteEverything;

    @Nonnull
    @Override
    public Integer call() throws Exception {
        if (paths == null || paths.isEmpty()) {
            return deleteAll();
        } else {
            return deleteByPath(paths);
        }
    }

    @Nonnull
    private Integer deleteAll() throws Exception {
        ThreescaleCmsClient client = topLevelCommand.getClient();

        if (!reallyDeleteEverything) {
            Log.warn("Potentially destructive command avoided; re-run the command with option `--yes-i-really-want-to-delete-the-entire-developer-portal` to continue anyway.");
            return 0;
        }

        topLevelCommand.getCmsObjects()
            .forEach(cmsObj -> {
                try {
                    client.delete(cmsObj);
                } catch (ApiException e) {
                    // TODO: Actually handle exceptions better
                    throw new RuntimeException(e);
                }
            });

        return 0;
    }

    @Nonnull
    private Integer deleteByPath(@Nonnull Collection<String> filenames) throws Exception {
        ThreescaleCmsClient client = topLevelCommand.getClient();
        Set<String> remainingFilenames = new HashSet<>(filenames);

        topLevelCommand.getCmsObjects().stream()
            .filter(cmsObject -> remainingFilenames.remove(cmsObjectPathKeyGenerator.generatePathKeyForObject(cmsObject)))
            .forEach(cmsObj -> {
                try {
                    // TODO: Remove sub-objects of sections that are specified
                    client.delete(cmsObj);
                } catch (ApiException e) {
                    // TODO: Actually handle exceptions better
                    throw new RuntimeException(e);
                }
            });

        Log.warn("Paths not removed: " + String.join(", ", remainingFilenames));

        return 0;
    }

}
