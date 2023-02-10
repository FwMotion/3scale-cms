package com.fwmotion.threescale.cms.cli;

import javax.inject.Inject;

import com.fwmotion.threescale.cms.cli.support.Context;
import com.fwmotion.threescale.cms.cli.util.ConfigurationContext;
import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import picocli.CommandLine.Command;

@Command(name = "current-context", description = "Displays the current context")
public class CurrentContextCommand implements Runnable {

    @Inject
    ConfigurationContext context;

    @Override
    public void run() {
        Context currentContext = context.getCurrentContext();
        String[][] data = new String[2][];
        data[0] = new String[]{ "Provider Domain", currentContext.getProviderDomain().toASCIIString() };
        data[1] = new String[]{ "Access Token", currentContext.getAccessToken() };

        String table = AsciiTable.getTable(AsciiTable.NO_BORDERS,
                new Column[]{
                        new Column().header("KEY").dataAlign(HorizontalAlign.LEFT),
                        new Column().header("VALUE").dataAlign(HorizontalAlign.LEFT)
                },
                data);

        System.out.println("Using context '" + context.getCurrentContextName() + "'");
        System.out.println();
        System.out.println(table);

    }}