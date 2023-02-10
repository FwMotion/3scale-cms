package com.fwmotion.threescale.cms.cli.support;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Context (URI domain, String accessToken) {
    public Context(
            @JsonProperty("domain") URI domain,
            @JsonProperty("accessToken") String accessToken) {
        this.domain = domain;
        this.accessToken = accessToken;
    }

    public URI getProviderDomain() {
        return domain;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public static Context defaultContext() {
        return new Context(null, null);
    }
}
