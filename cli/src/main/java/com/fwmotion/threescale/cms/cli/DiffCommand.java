package com.fwmotion.threescale.cms.cli;

import com.fwmotion.threescale.cms.cli.support.CmsObjectPathKeyGenerator;
import com.fwmotion.threescale.cms.cli.support.LocalRemoteObjectTreeComparator;
import com.fwmotion.threescale.cms.cli.support.LocalRemoteTreeComparisonDetails;
import com.fwmotion.threescale.cms.model.CmsLayout;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
    header = "Diff 3scale CMS with Local Files",
    name = "diff",
    description = "Display the difference between CMS and local files"
)
public class DiffCommand extends CommandBase implements Callable<Integer> {

    @Inject
    LocalRemoteObjectTreeComparator treeComparator;

    @Inject
    CmsObjectPathKeyGenerator cmsObjectPathKeyGenerator;

    @CommandLine.ParentCommand
    private TopLevelCommand topLevelCommand;

    @Override
    public Integer call() throws Exception {
        showDiff(false);

        return 0;
    }

    @CommandLine.Command(
        name = "details"
    )
    public Integer diffDetails() throws Exception {
        showDiff(true);

        return 0;
    }

    private void showDiff(boolean includeDetails) throws Exception {
        InfoCommand.displayCmsUrl(topLevelCommand.getProviderDomain());

        LocalRemoteTreeComparisonDetails details = treeComparator.compareLocalAndRemoteCmsObjectTrees(
            topLevelCommand.getCmsObjects().stream(),
            topLevelCommand.getRootDirectory(),
            true);

        CmsLayout defaultLayout = details.getDefaultLayout().orElse(null);
        if (defaultLayout == null) {
            Log.info("No default layout found!");
        } else {
            InfoCommand.displayDefaultLayout(defaultLayout,
                cmsObjectPathKeyGenerator.generatePathKeyForObject(defaultLayout));
        }

        Log.info("");

        if (includeDetails) {
            if (!details.getRemotePathsMissingInLocal().isEmpty()) {
                Log.info("Files that will be created locally on 'download'\n" +
                    InfoCommand.listPaths(details.getRemotePathsMissingInLocal().stream()));
            }

            if (!details.getRemoteObjectsNewerThanLocal().isEmpty()) {
                Log.info("Files that exist locally that will be updated on 'download'\n" +
                    InfoCommand.listPaths(details.getRemoteObjectsNewerThanLocal().stream()));
            }

            if (!details.getLocalPathsMissingInRemote().isEmpty()) {
                Log.info("Files that exist locally will be created in CMS on 'upload'\n" +
                    InfoCommand.listPaths(details.getLocalPathsMissingInRemote().stream()));
            }

            if (!details.getLocalObjectsNewerThanRemote().isEmpty()) {
                Log.info("Files that have been modified locally and will be updated in CMS on 'upload'\n" +
                    InfoCommand.listPaths(details.getLocalObjectsNewerThanRemote().stream()));
            }
        }

        Log.info("Summary:\n" +
            details.getRemotePathsMissingInLocal().size() + " files to be created locally\n" +
            details.getRemoteObjectsNewerThanLocal().size() + " files to be updated locally\n" +
            details.getLocalPathsMissingInRemote().size() + " files to be created on CMS\n" +
            details.getLocalObjectsNewerThanRemote().size() + " files to be updated on CMS");
    }

}
