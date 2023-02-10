package com.fwmotion.threescale.cms.cli.util;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fwmotion.threescale.cms.cli.support.Configuration;
import com.fwmotion.threescale.cms.cli.support.Context;

@ApplicationScoped
public class ConfigurationContext {
    private static final String CONFIG_FILE = ".3scale-cms";
    private final File configFile;
    private final ObjectMapper objectMapper;

    public ConfigurationContext() {
        this(new File(System.getProperty("user.home")));
    }

    public ConfigurationContext(File configDirectory) {
        this.configFile = new File(configDirectory, CONFIG_FILE);
        this.objectMapper = JsonMapper
                .builder()
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(SerializationFeature.INDENT_OUTPUT, true)
                .build();
    }

    public void setContext(String contextName, Context context) {
        if (!configFile.exists()) {
            tryWriteConfiguration(new Configuration(contextName).addConfigurationContext(contextName, context));

            return;
        }

        var configuration = tryReadConfiguration();

        tryWriteConfiguration(configuration.addConfigurationContext(contextName, context));
    }

    public Context getContext(String contextName) {
        if (!configFile.exists()) {
            warnAboutMissingConfigFile();

            return Context.defaultContext();
        }

        var configuration = tryReadConfiguration();

        return configuration.configurationContexts().get(contextName);
    }

    private void warnAboutMissingConfigFile() {
        System.out.println("No configuration context has been defined." +
                " Run '3scale-cms config set-context <context_name> --provider-domain=<provider_domain> --access-token=<access_token>].' to create a context.");
    }

    public Map<String, Context> getContexts() {
        Map<String, Context> contexts = new LinkedHashMap<>();
        if (!configFile.exists()) {
            warnAboutMissingConfigFile();

            return contexts;
        }

        var configuration = tryReadConfiguration();
        return configuration.configurationContexts();
    }

    public Context getCurrentContext() {
        if (!configFile.exists()) {
            warnAboutMissingConfigFile();

            return Context.defaultContext();
        }

        var configuration = tryReadConfiguration();

        return configuration.configurationContexts().get(configuration.getCurrentContext());
    }

    public String getCurrentContextName() {
        if (!configFile.exists()) {
            warnAboutMissingConfigFile();

            return "";
        }

        var configuration = tryReadConfiguration();

        return configuration.getCurrentContext();
    }

    public boolean setCurrentContext(String contextName) {
        if (!configFile.exists()) {
            warnAboutMissingConfigFile();
            return false;
        }

        var configuration = tryReadConfiguration();

        if (!configuration.configurationContexts().containsKey(contextName)) {
            return false;
        }

        configuration.setCurrentContext(contextName);

        tryWriteConfiguration(configuration);

        return true;
    }

    private Configuration tryReadConfiguration() {
        try {
            return objectMapper.readValue(configFile, Configuration.class);
        }
        catch (IOException e) {
            throw new RuntimeException("Couldn't read configuration file ~/" + CONFIG_FILE + ". Create a new one by " +
                    "running '3scale-cms config set-context <context_name> --provider-domain=<provider_domain> --access-token=<access_token>].' to create a context. ",
                    e);
        }
    }

    private void tryWriteConfiguration(Configuration configuration) {
        try {
            objectMapper.writeValue(configFile, configuration);
        }
        catch (IOException e) {
            throw new RuntimeException("Couldn't write configuration file " + configFile + ". Create a new one by " +
                    "running un '3scale-cms config set-context <context_name> --provider-domain=<provider_domain> --access-token=<access_token>].' to create a context.",
                    e);
        }
    }
}
