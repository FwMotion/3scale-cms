package com.fwmotion.threescale.cms.cli;

import com.fwmotion.threescale.cms.model.CmsLayout;
import com.fwmotion.threescale.cms.model.CmsObject;
import io.quarkus.logging.Log;
import org.apache.http.client.utils.URIBuilder;
import picocli.CommandLine;

import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(
    header = "3scale CMS Information",
    name = "info",
    description = "Display information on 3scale CMS and local files"
)
public class InfoCommand extends CommandBase implements Callable<Integer> {

    @CommandLine.ParentCommand
    private TopLevelCommand topLevelCommand;

    @Override
    public Integer call() throws Exception {
        URIBuilder uriBuilder = new URIBuilder(topLevelCommand.getProviderDomain());
        String targetPath = (uriBuilder.getPath() + "/admin/api/cms")
            .replaceAll("/+", "/");
        String cmsUrl = uriBuilder.setPath(targetPath).toString();
        Log.info("Contacting CMS at " + cmsUrl + " to get content list");

        List<CmsObject> allObjects = topLevelCommand.getCmsObjects();
        CmsLayout defaultLayout = topLevelCommand.getDefaultLayout();

        Log.info("The layout '" + defaultLayout.getSystemName() + "' was selected as the default layout for uploading new pages");
        Log.info(allObjects.size() + " content elements found in CMS");

        // TODO: Find the number of implicit

        return 0;
    }

    @CommandLine.Command(
        name = "details"
    )
    public Integer infoDetails() {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }

}
