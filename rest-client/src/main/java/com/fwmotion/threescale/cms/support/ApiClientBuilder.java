package com.fwmotion.threescale.cms.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fwmotion.threescale.cms.mixins.EnumHandlerMixIn;
import com.redhat.threescale.rest.cms.ApiClient;
import com.redhat.threescale.rest.cms.model.EnumHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;

public final class ApiClientBuilder {

    private ApiClientBuilder() {
    }

    public static ApiClient buildApiClient(CloseableHttpClient httpClient) {
        ApiClient apiClient = new ApiClient(httpClient);

        applyMixIns(apiClient.getObjectMapper());

        return apiClient;
    }

    /**
     * openapi-generator doesn't generate things exactly as intended in some
     * cases, so use mixins to fix
     *
     * @param objectMapper the Jackson {@link ObjectMapper} to apply MixIns
     */
    static void applyMixIns(ObjectMapper objectMapper) {
        objectMapper.addMixIn(EnumHandler.class, EnumHandlerMixIn.class);
    }

}
