package com.fwmotion.threescale.cms.cli;

import com.fwmotion.threescale.cms.cli.support.CmsObjectPathKeyGenerator;
import com.fwmotion.threescale.cms.cli.support.LocalRemoteObjectTreeComparator;
import com.fwmotion.threescale.cms.cli.support.LocalRemoteTreeComparisonDetails;
import com.fwmotion.threescale.cms.model.CmsLayout;
import com.fwmotion.threescale.cms.model.CmsObject;
import io.quarkus.logging.Log;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import picocli.CommandLine;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandLine.Command(
    header = "3scale CMS Information",
    name = "info",
    description = "Display information on 3scale CMS and local files"
)
public class InfoCommand extends CommandBase implements Callable<Integer> {

    @Inject
    LocalRemoteObjectTreeComparator treeComparator;

    @Inject
    CmsObjectPathKeyGenerator cmsObjectPathKeyGenerator;

    @CommandLine.ParentCommand
    private TopLevelCommand topLevelCommand;

    @Override
    public Integer call() throws Exception {
        showInfo(false);

        return 0;
    }

    @CommandLine.Command(
        name = "details"
    )
    public Integer infoDetails() throws Exception {
        showInfo(true);

        return 0;
    }

    private void showInfo(boolean includeDetails) throws Exception {
        displayCmsUrl(topLevelCommand.getProviderDomain());

        List<CmsObject> allRemoteObjects = topLevelCommand.getCmsObjects();
        LocalRemoteTreeComparisonDetails details = treeComparator.compareLocalAndRemoteCmsObjectTrees(
            allRemoteObjects.stream(),
            topLevelCommand.getRootDirectory(),
            true);

        CmsLayout defaultLayout = details.getDefaultLayout().orElse(null);
        if (defaultLayout == null) {
            Log.info("No default layout found!");
        } else {
            InfoCommand.displayDefaultLayout(defaultLayout,
                cmsObjectPathKeyGenerator.generatePathKeyForObject(defaultLayout));
        }

        Log.info(allRemoteObjects.size() + " items found in CMS");
        if (includeDetails) {
            if (!allRemoteObjects.isEmpty()) {
                int longestPath = details.getRemoteObjectsByCmsPath().keySet().stream()
                    .mapToInt(String::length)
                    .max()
                    .orElse(0);

                Log.info(
                    details.getRemoteObjectsByCmsPath().entrySet()
                        .stream()
                        .sequential()
                        .sorted((left, right) -> comparePaths(left.getKey(), right.getKey()))
                        .map(pair -> "\t'" + StringUtils.rightPad(pair.getKey() + "'", longestPath + 1)
                            + " "
                            + pair.getValue().getType())
                        .collect(Collectors.joining("\n")));
            }
            Log.info("");
        }

        Log.info(details.getLocalPathsIgnored().size()
            + " ignored local files (matching patterns in '.cmsignore')");
        if (includeDetails) {
            if (!details.getLocalPathsIgnored().isEmpty()) {
                Log.info(
                    listPaths(details.getLocalPathsIgnored().stream()));
            }
            Log.info("");
        }

        Log.info(details.getLocalObjectsByCmsPath().size()
            + " (non-ignored) local files");
        if (includeDetails) {
            if (!details.getLocalObjectsByCmsPath().isEmpty()) {
                int longestPath = details.getLocalObjectsByCmsPath().keySet().stream()
                    .mapToInt(String::length)
                    .max()
                    .orElse(0);

                Log.info(
                    details.getLocalObjectsByCmsPath().entrySet()
                        .stream()
                        .sequential()
                        .sorted((left, right) -> comparePaths(left.getKey(), right.getKey()))
                        .map(pair -> "\t'" + StringUtils.rightPad(pair.getKey() + "'", longestPath + 1)
                            + " "
                            + pair.getValue().getLeft().getType())
                        .collect(Collectors.joining("\n")));
            }
            Log.info("");
        }

        Log.info(details.getImplicitSectionPaths().size()
            + " implicit folders due to file/template system_names containing '/'");
        if (includeDetails) {
            if (!details.getImplicitSectionPaths().isEmpty()) {
                Log.info(
                    listPaths(details.getImplicitSectionPaths().stream()));
            }
            Log.info("");
        }

    }

    static void displayCmsUrl(String providerDomain) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(providerDomain);
        String providerPath = uriBuilder.getPath();

        String targetPath;
        if (providerPath == null) {
            targetPath = "/admin/api/cms";
        } else {
            targetPath = providerPath + "/admin/api/cms";
        }
        targetPath = targetPath.replaceAll("/+", "/");

        String cmsUrl = uriBuilder.setPath(targetPath).toString();
        Log.info("Contacting CMS at " + cmsUrl + " to get content list");
    }

    static void displayDefaultLayout(CmsLayout defaultLayout, String defaultLayoutPathKey) {
        Log.info("The layout '"
            + defaultLayout.getSystemName()
            + "' in file '"
            + defaultLayoutPathKey
            + "' was selected as the default layout for uploading new pages");
    }

    static String listPaths(Stream<String> pathStream) {
        return pathStream
            .sequential()
            .sorted(InfoCommand::comparePaths)
            .map(path -> "\t'" + path + "'")
            .collect(Collectors.joining("\n"));
    }

    static int comparePaths(@Nonnull String leftString,
                             @Nonnull String rightString) {
        return Path.of("/", StringUtils.split(leftString, '/'))
            .compareTo(Path.of("/", StringUtils.split(rightString, '/')));
    }

}
