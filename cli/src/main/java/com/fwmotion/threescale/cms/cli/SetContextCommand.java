package com.fwmotion.threescale.cms.cli;

import java.net.URI;
import java.util.concurrent.Callable;

import com.fwmotion.threescale.cms.cli.support.Context;
import com.fwmotion.threescale.cms.cli.util.ConfigurationContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "set-context", description = "Configures the specified context with the provided arguments")
public class SetContextCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Context name")
    String contextName;

    @Option(names = { "--domain" }, description = "Base URL of the 3scale admin portal to connect to; for example: https://3scale-admin.apps.example.com/")
    String domain;

    @Option(names = { "--access-token" }, description = "Access token for full control of the target tenant. The access token must be granted permissions to both Account Management API and the Content Management API")
    String accessToken;
    
    @Override
    public Integer call() throws Exception {
        ConfigurationContext context = new ConfigurationContext();

        context.setContext(contextName, new Context(URI.create(domain), accessToken));
        System.out.println("Configured context " + contextName);

        if (!context.getCurrentContextName().equals(contextName)) {
            System.out.println("Run 3scale-cms config use-context " + contextName + " for using this context");
        }

        return 0;
    }

}
