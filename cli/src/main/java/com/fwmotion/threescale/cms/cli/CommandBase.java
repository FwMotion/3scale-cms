package com.fwmotion.threescale.cms.cli;

import com.fwmotion.threescale.cms.cli.version.VersionProperties;
import com.fwmotion.threescale.cms.cli.version.VersionProvider;
import picocli.CommandLine;

import javax.inject.Inject;

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
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class
)
public class CommandBase {

    // This is just included so the properties bean will get loaded
    @Inject
    VersionProperties versionProperties;

}
