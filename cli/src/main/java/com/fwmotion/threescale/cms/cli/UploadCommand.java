package com.fwmotion.threescale.cms.cli;

import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

@CommandLine.Command(
    header = "Upload 3scale CMS Content",
    name = "upload",
    description = "Upload all content, specified file or all specified folder contents to CMS"
)
public class UploadCommand extends CommandBase implements Callable<Integer> {

    @CommandLine.ParentCommand
    private TopLevelCommand topLevelCommand;

    @CommandLine.Parameters(
        index = "0",
        arity = "0..*"
    )
    private File file;

    @CommandLine.Option(
        names = {"--layout"},
        arity = "1",
        paramLabel = "layout_file_name",
        description = "Specify layout for new (not updated) pages"
    )
    private String layoutFilename;

    @Override
    public Integer call() throws Exception {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }

}
