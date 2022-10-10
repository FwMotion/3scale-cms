package com.fwmotion.threescale.cms.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fwmotion.threescale.cms.mixins.TemplateListMixIn;
import com.redhat.threescale.rest.cms.XmlEnabledApiClient;
import com.redhat.threescale.rest.cms.model.TemplateList;
import org.apache.http.impl.client.CloseableHttpClient;

public final class ApiClientBuilder {

    private ApiClientBuilder() {
    }

    public static XmlEnabledApiClient buildApiClient(CloseableHttpClient httpClient) {
        XmlEnabledApiClient xmlApiClient = new XmlEnabledApiClient(httpClient);

        applyMixIns(xmlApiClient.getXmlMapper());
        applyMixIns(xmlApiClient.getObjectMapper());

        return xmlApiClient;
    }

    /**
     * openapi-generator doesn't generate things exactly right in some cases,
     * so use mixins to fix
     *
     * @param objectMapper the Jackson {@link ObjectMapper} to apply MixIns
     */
    static void applyMixIns(ObjectMapper objectMapper) {
        objectMapper.addMixIn(TemplateList.class, TemplateListMixIn.class);
    }

}
