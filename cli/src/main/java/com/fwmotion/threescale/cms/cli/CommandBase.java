package com.fwmotion.threescale.cms.cli;

import com.fwmotion.threescale.cms.cli.version.VersionProperties;
import com.fwmotion.threescale.cms.cli.version.VersionProvider;
import jakarta.inject.Inject;
import picocli.CommandLine;

@CommandLine.Command(
    headerHeading = "%n",
    synopsisHeading = "%n@|green,bold USAGE|@%n    ",
    parameterListHeading = "%n@|green,bold PARAMETERS|@%n",
    descriptionHeading = "%n@|green,bold DESCRIPTION|@%n    ",
    optionListHeading = "%n@|green,bold OPTIONS|@%n",
    synopsisSubcommandLabel = "[SUBCOMMAND]",
    commandListHeading = "%n@|green,bold SUBCOMMANDS|@%n",
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
