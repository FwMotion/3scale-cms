package com.fwmotion.threescale.cms.cli;

import com.fwmotion.threescale.cms.ThreescaleCmsClient;
import com.fwmotion.threescale.cms.ThreescaleCmsClientFactory;
import com.fwmotion.threescale.cms.model.CmsLayout;
import com.fwmotion.threescale.cms.model.CmsObject;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

@TopCommand
@CommandLine.Command(
    header = {"3scale Content Management System CLI Tool"},
    name = "3scale-cms",
    subcommands = {
        DownloadCommand.class,
        UploadCommand.class,
        DeleteCommand.class,
        InfoCommand.class,
        DiffCommand.class
    },
    synopsisSubcommandLabel = "[SUBCOMMAND]",
    commandListHeading = "%n@|red,bold SUBCOMMANDS|@%n"
)
public class TopLevelCommand extends CommandBase {

    @CommandLine.Option(
        names = {"-k", "--insecure"},
        description = "Proceed with server connections that fail TLS certificate validation"
    )
    private boolean useInsecureConnections;

    @CommandLine.Parameters(
        index = "0",
        paramLabel = "PROVIDER_KEY",
        arity = "1",
        description = "Provider Key for full control of the target tenant. This will be ignored if --access-token is specified."
    )
    private String providerKey;

    @CommandLine.Parameters(
        index = "1",
        paramLabel = "PROVIDER_DOMAIN",
        arity = "1",
        description = "The base URL of the admin portal; for example: %n  https://3scale-admin.apps.example.com/"
    )
    private String providerDomain;

    @CommandLine.Option(
        names = {"-a", "--access-token"},
        paramLabel = "ACCESS_TOKEN",
        arity = "1",
        description = "Use an access token instead of a provider key. The access token must be granted permissions to both Account Management API and the hidden Content Management API"
    )
    private String accessToken;

    private ThreescaleCmsClientFactory factory;
    private List<CmsObject> cmsObjects;

    public String getProviderDomain() {
        return providerDomain;
    }

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

}
