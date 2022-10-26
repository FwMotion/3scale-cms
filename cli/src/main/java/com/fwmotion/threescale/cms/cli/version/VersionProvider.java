package com.fwmotion.threescale.cms.cli.version;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import picocli.CommandLine;

public class VersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() {

        VersionProperties config;
        try (InstanceHandle<VersionProperties> versionHandle = Arc.container().instance(VersionProperties.class)) {
            config = versionHandle
                .orElse(new VersionProperties("-unknown", "-unknown"));
        }

        return new String[]{
            config.getApplicationName() + " v" + config.getApplicationVersion(),
        };
    }
}
