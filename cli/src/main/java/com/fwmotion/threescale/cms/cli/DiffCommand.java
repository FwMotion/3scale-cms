package com.fwmotion.threescale.cms.cli;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
    header = "Diff 3scale CMS with Local Files",
    name = "diff",
    description = "Display the difference between CMS and local files"
)
public class DiffCommand extends CommandBase implements Callable<Integer> {

    @CommandLine.ParentCommand
    private TopLevelCommand topLevelCommand;

    @Override
    public Integer call() throws Exception {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @CommandLine.Command(
        name = "details"
    )
    public Integer infoDetails() {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }

}
