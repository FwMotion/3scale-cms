package com.fwmotion.threescale.cms.cli;

import picocli.CommandLine.Command;

@Command(name = "config", subcommands = { SetContextCommand.class, GetContextsCommand.class,
    CurrentContextCommand.class, UseContextCommand.class }, description = "Sets or retrieves the configuration of the tenant"

)
public class ConfigCommand {
}
