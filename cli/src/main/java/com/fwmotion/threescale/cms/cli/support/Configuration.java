package com.fwmotion.threescale.cms.cli.support;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Configuration {

    private String currentContext;
    private final Map<String, Context> configurationContexts = new LinkedHashMap<>();

    public Configuration(@JsonProperty("currentContext") String currentContext) {
        this.currentContext = currentContext;
    }

    public String getCurrentContext() {
        return currentContext;
    }

    public void setCurrentContext(String currentContext) {
        this.currentContext = currentContext;
    }

    @JsonAnyGetter
    public Map<String, Context> configurationContexts() {
        return configurationContexts;
    }

    @JsonAnySetter
    public Configuration addConfigurationContext(String contextName, Context configuration) {
        configurationContexts.put(contextName, configuration);
        return this;
    }
}
