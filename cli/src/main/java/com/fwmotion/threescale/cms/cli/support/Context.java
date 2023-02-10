package com.fwmotion.threescale.cms.cli.support;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Context (URI providerDomain, String accessToken) {
    public Context(
            @JsonProperty("providerDomain") URI providerDomain,
            @JsonProperty("accessToken") String accessToken) {
        this.providerDomain = providerDomain;
        this.accessToken = accessToken;
    }

    public URI getProviderDomain() {
        return providerDomain;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public static Context defaultContext() {
        return new Context(null, null);
    }
}
