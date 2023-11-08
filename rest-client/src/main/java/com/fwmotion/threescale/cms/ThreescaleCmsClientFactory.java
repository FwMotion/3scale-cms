package com.fwmotion.threescale.cms;

import com.fwmotion.threescale.cms.exception.ThreescaleCmsNonApiException;
import com.fwmotion.threescale.cms.support.ApiClientBuilder;
import com.redhat.threescale.rest.cms.ApiClient;
import com.redhat.threescale.rest.cms.auth.ApiKeyAuth;
import com.redhat.threescale.rest.cms.auth.Authentication;
import jakarta.annotation.Nonnull;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContexts;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class ThreescaleCmsClientFactory implements AutoCloseable {

    private String baseUrl;
    private boolean useInsecureConnections;
    private String providerKey;
    private String accessToken;

    private HttpClientConnectionManager httpClientConnectionManager;
    private CloseableHttpClient httpClient;

    private void tryCloseHttpClient() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                throw new ThreescaleCmsNonApiException("Couldn't close old HTTP client", e);
            }
        }
        httpClient = null;
    }

    private CloseableHttpClient getHttpClient() {
        if (httpClient == null) {
            if (isUseInsecureConnections()) {
                try {
                    SSLConnectionSocketFactory connectionSocketFactory = new SSLConnectionSocketFactory(SSLContexts.custom()
                        .loadTrustMaterial(null, new TrustAllStrategy())
                        .build(),
                        NoopHostnameVerifier.INSTANCE);

                    httpClientConnectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(connectionSocketFactory)
                        .build();
                    httpClient = HttpClients.custom()
                        .setConnectionManager(httpClientConnectionManager)
                        .build();
                } catch (NoSuchAlgorithmException | KeyManagementException |
                         KeyStoreException e) {
                    throw new ThreescaleCmsNonApiException("Unable to create insecure HttpClient", e);
                }
            } else {
                httpClient = HttpClients.createDefault();
            }
        }

        return httpClient;
    }

    private ApiClient newApiClient() {
        ApiClient apiClient = ApiClientBuilder.buildApiClient(getHttpClient());

        apiClient.setBasePath(baseUrl);

        if (providerKey != null) {
            Authentication providerKeyAuth = apiClient.getAuthentication("provider_key");
            ((ApiKeyAuth) providerKeyAuth).setApiKey(apiClient.escapeString(providerKey));
        } else if (accessToken != null) {
            Authentication accessTokenAuth = apiClient.getAuthentication("access_token");
            ((ApiKeyAuth) accessTokenAuth).setApiKey(apiClient.escapeString(accessToken));
        } else {
            throw new ThreescaleCmsNonApiException("Authentication not set for 3scale CMS client; must provide one of: providerKey, accessToken");
        }

        return apiClient;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isUseInsecureConnections() {
        return useInsecureConnections;
    }

    public void setUseInsecureConnections(boolean useInsecureConnections) {
        if (this.useInsecureConnections != useInsecureConnections) {
            tryCloseHttpClient();
        }
        this.useInsecureConnections = useInsecureConnections;
    }

    public String getProviderKey() {
        return providerKey;
    }

    public void setProviderKey(String providerKey) {
        this.providerKey = providerKey;
        this.accessToken = null;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        this.providerKey = null;
    }

    @Nonnull
    public ThreescaleCmsClient getThreescaleCmsClient() {
        return new ThreescaleCmsClientImpl(newApiClient());
    }

    @Override
    public void close() throws Exception {
        if (httpClientConnectionManager != null) {
            httpClientConnectionManager.close();
        }
        if (httpClient != null) {
            httpClient.close();
        }
    }
}
