package com.fwmotion.threescale.cms.cli;

import com.fwmotion.threescale.cms.ThreescaleCmsClient;
import com.fwmotion.threescale.cms.ThreescaleCmsClientFactory;
import com.fwmotion.threescale.cms.cli.util.ConfigurationContext;
import com.fwmotion.threescale.cms.model.CmsObject;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

@TopCommand
@CommandLine.Command(header = {
        "3scale Content Management System CLI Tool" }, name = "3scale-cms", description = "Tool for interacting with 3scale's Content Management "
                +
                "System for managing content and templates that will be used to " +
                "render a tenant's Developer Portal", subcommands = {
                        ConfigCommand.class,
                        InfoCommand.class,
                        DiffCommand.class,
                        DownloadCommand.class,
                        UploadCommand.class,
                        DeleteCommand.class,
                }, synopsisSubcommandLabel = "[COMMAND] ", commandListHeading = "%n@|green,bold COMMANDS|@%n")
public class TopLevelCommand extends CommandBase {

    private final ConfigurationContext context;

    @Inject
    public TopLevelCommand(ConfigurationContext context) {
        this.context = context;
    }

    // Hack : Picocli currently require an empty constructor to generate the
    // completion file
    public TopLevelCommand() {
        context = new ConfigurationContext();
    }

    @CommandLine.Option(names = { "-k", "--insecure" }, description = "Proceed with server connections that fail TLS " +
            "certificate validation")
    private boolean useInsecureConnections;

    @CommandLine.Option(names = { "-d",
            "--directory" }, paramLabel = "DIRECTORY", arity = "1", description = "Specify local directory path for determining files "
                    +
                    "to upload, download, or compare between local filesystem and " +
                    "3scale CMS content")
    private File rootDirectory;

    private ThreescaleCmsClientFactory factory;
    private List<CmsObject> cmsObjects;

    public ThreescaleCmsClient getClient() {
        if (factory == null) {
            factory = new ThreescaleCmsClientFactory();
            factory.setBaseUrl(context.getCurrentContext().getProviderDomain().toASCIIString());
            factory.setAccessToken(context.getCurrentContext().getAccessToken());
            factory.setUseInsecureConnections(useInsecureConnections);
        }

        return factory.getThreescaleCmsClient();
    }

    public List<CmsObject> getCmsObjects() {
        if (cmsObjects == null) {
            cmsObjects = getClient().listAllCmsObjects();
        }

        return new ArrayList<>(cmsObjects);
    }

    public File getRootDirectory() throws IOException {
        if (rootDirectory == null) {
            String threescaleCmsRoot = System.getenv("THREESCALE_CMS_ROOT");
            if (StringUtils.isNotBlank(threescaleCmsRoot)) {
                rootDirectory = new File(threescaleCmsRoot);
            } else {
                rootDirectory = new File(".");
            }
        }

        if (!rootDirectory.getCanonicalFile().isDirectory()) {
            throw new IllegalStateException("Specified root directory is not a directory");
        }

        return rootDirectory;
    }

}
