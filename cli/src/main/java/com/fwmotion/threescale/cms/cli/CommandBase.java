package com.fwmotion.threescale.cms.cli;

import picocli.CommandLine;

@CommandLine.Command(
    headerHeading = "%n",
    synopsisHeading = "%n@|red,bold USAGE|@%n    ",
    parameterListHeading = "%n@|red,bold PARAMETERS|@%n",
    descriptionHeading = "%n@|red,bold DESCRIPTION|@%n    ",
    optionListHeading = "%n@|red,bold OPTIONS|@%n",
    synopsisSubcommandLabel = "[SUBCOMMAND]",
    commandListHeading = "%n@|red,bold SUBCOMMANDS|@%n",
    abbreviateSynopsis = true,
    mixinStandardHelpOptions = true,
    showDefaultValues = true,
    usageHelpAutoWidth = true
)
public class CommandBase {
}
