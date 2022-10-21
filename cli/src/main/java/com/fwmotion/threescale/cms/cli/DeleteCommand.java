package com.fwmotion.threescale.cms.cli;

import com.fwmotion.threescale.cms.ThreescaleCmsClient;
import com.redhat.threescale.rest.cms.ApiException;
import io.quarkus.logging.Log;
import picocli.CommandLine;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(
    header = "Delete 3scale CMS Content",
    name = "delete",
    description = "Delete all possible content in the specified CMS"
)
public class DeleteCommand extends CommandBase implements Callable<Integer> {

    @CommandLine.ParentCommand
    private TopLevelCommand topLevelCommand;

    @CommandLine.Parameters(
        index = "0",
        arity = "0..*"
    )
    private List<String> filenames;

    @CommandLine.Option(
        names = "--yes-i-really-want-to-delete-the-entire-developer-portal",
        hidden = true,
        defaultValue = "false"
    )
    private boolean reallyDeleteEverything;

    @Nonnull
    @Override
    public Integer call() throws Exception {
        if (filenames == null || filenames.isEmpty()) {
            return deleteAll();
        } else {
            return deleteBySystemName(filenames);
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
    private Integer deleteBySystemName(@Nonnull List<String> filenames) throws Exception {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }

}
