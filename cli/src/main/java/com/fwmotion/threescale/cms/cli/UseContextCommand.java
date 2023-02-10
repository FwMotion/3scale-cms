package com.fwmotion.threescale.cms.cli;

import com.fwmotion.threescale.cms.cli.util.ConfigurationContext;
import com.fwmotion.threescale.cms.cli.util.ContextNameCompletions;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "use-context", description = "Configures 3scale-cmx to use the specified context")
public class UseContextCommand implements Runnable {

    @Parameters(index = "0", description = "Context name", completionCandidates = ContextNameCompletions.class)
    String contextName;
    
    @Override
    public void run() {
        ConfigurationContext context = new ConfigurationContext();

        if (context.getCurrentContextName().equals(contextName)) {
            System.out.println("Already using context " + contextName);
        }
        else {
            boolean success = context.setCurrentContext(contextName);

            if (success) {
                System.out.println("Using context '" + contextName + "'");
            }
            else {
                System.out.println("Couldn't change context; create a context named " + contextName + " first");
            }
        }
    }

    
}
