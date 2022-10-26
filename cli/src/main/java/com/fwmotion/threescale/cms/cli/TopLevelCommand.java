package com.fwmotion.threescale.cms.cli;

import com.fwmotion.threescale.cms.ThreescaleCmsClient;
import com.fwmotion.threescale.cms.ThreescaleCmsClientFactory;
import com.fwmotion.threescale.cms.model.CmsLayout;
import com.fwmotion.threescale.cms.model.CmsObject;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@TopCommand
@CommandLine.Command(
    header = {"3scale Content Management System CLI Tool"},
    name = "3scale-cms",
    description = "Tool for interacting with 3scale's Content Management " +
        "System for managing content and templates that will be used to " +
        "render a tenant's Developer Portal",
    subcommands = {
        InfoCommand.class,
        DiffCommand.class,
        DownloadCommand.class,
        UploadCommand.class,
        DeleteCommand.class,
    },
    synopsisSubcommandLabel = "[COMMAND] ",
    commandListHeading = "%n@|red,bold COMMANDS|@%n"
)
public class TopLevelCommand extends CommandBase {

    @CommandLine.Option(
        names = {"-k", "--insecure"},
        description = "Proceed with server connections that fail TLS " +
            "certificate validation"
    )
    private boolean useInsecureConnections;

    @CommandLine.Parameters(
        index = "0",
        paramLabel = "PROVIDER_KEY",
        arity = "1",
        description = "Provider Key for full control of the target tenant. " +
            "This will be ignored if --access-token is specified."
    )
    private String providerKey;

    @CommandLine.Parameters(
        index = "1",
        paramLabel = "PROVIDER_DOMAIN",
        arity = "1",
        description = {
            "The base URL of the admin portal; for example:",
            "https://3scale-admin.apps.example.com/"
        }
    )
    private String providerDomain;

    @CommandLine.Option(
        names = {"-a", "--access-token"},
        paramLabel = "ACCESS_TOKEN",
        arity = "1",
        description = "Use an access token instead of a provider key. The " +
            "access token must be granted permissions to both " +
            "Account Management API and the hidden Content Management API"
    )
    private String accessToken;

    @CommandLine.Option(
        names = {"-d", "--directory"},
        paramLabel = "DIRECTORY",
        arity = "1",
        description = "Specify local directory path for determining files " +
            "to upload, download, or compare between local filesystem and " +
            "3scale CMS content"
    )
    private File rootDirectory;

    private ThreescaleCmsClientFactory factory;
    private List<CmsObject> cmsObjects;

    public ThreescaleCmsClient getClient() {
        if (factory == null) {
            factory = new ThreescaleCmsClientFactory();
            factory.setBaseUrl(providerDomain);
            if (StringUtils.isNotBlank(accessToken)) {
                factory.setAccessToken(accessToken);
            } else {
                factory.setProviderKey(providerKey);
            }
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

    public CmsLayout getDefaultLayout() {
        return getCmsObjects().stream()
            .filter(cmsObj -> cmsObj instanceof CmsLayout)
            .findFirst()
            .map(cmsObj -> (CmsLayout) cmsObj)
            .orElseThrow(() -> new IllegalStateException("Couldn't find any layout to use as default"));
    }

    public String getProviderDomain() {
        return providerDomain;
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
