package com.fwmotion.threescale.cms.cli;

import java.util.Map;

import javax.inject.Inject;

import com.fwmotion.threescale.cms.cli.support.Context;
import com.fwmotion.threescale.cms.cli.util.ConfigurationContext;
import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import picocli.CommandLine.Command;

@Command(name = "get-contexts", description = "Get all contexts")
public class GetContextsCommand implements Runnable {

    @Inject
    ConfigurationContext configContext;
    
    @Override
    public void run() {
        Map<String, Context> contexts = configContext.getContexts();
        String current = configContext.getCurrentContextName();

        String[][] data = contexts.entrySet()
                .stream()
                .map(e -> new String[]{ e.getKey() + (e.getKey().equals(current) ? "*" : ""), e.getValue().getProviderDomain().toASCIIString() })
                .toArray(size -> new String[size][]);

        String table = AsciiTable.getTable(AsciiTable.NO_BORDERS,
                new Column[]{
                        new Column().header("NAME").dataAlign(HorizontalAlign.LEFT),
                        new Column().header("PROVIDER DOMAIN URI").dataAlign(HorizontalAlign.LEFT)
                },
                data);

        System.out.println(table);

    }}
