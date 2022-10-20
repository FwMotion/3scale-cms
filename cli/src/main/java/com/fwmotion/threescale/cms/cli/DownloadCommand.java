package com.fwmotion.threescale.cms.cli;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
    header = "Download 3scale CMS Content",
    name = "download",
    description = "Download all contents of the specified CMS to the local folder"
)
public class DownloadCommand extends CommandBase implements Callable<Integer> {

    @CommandLine.ParentCommand
    private TopLevelCommand topLevelCommand;

    @Override
    public Integer call() throws Exception {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }

}
