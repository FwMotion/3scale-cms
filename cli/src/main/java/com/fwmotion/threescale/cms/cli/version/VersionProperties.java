package com.fwmotion.threescale.cms.cli.version;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class VersionProperties {

    private final String applicationName;
    private final String applicationVersion;

    public VersionProperties(
        @ConfigProperty(name = "quarkus.application.name", defaultValue = "-unknown")
        String applicationName,
        @ConfigProperty(name = "quarkus.application.version", defaultValue = "-unknown")
        String applicationVersion) {
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

}

