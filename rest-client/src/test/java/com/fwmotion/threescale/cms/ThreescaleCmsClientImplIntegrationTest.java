package com.fwmotion.threescale.cms;

import com.fwmotion.threescale.cms.model.CmsObject;
import com.fwmotion.threescale.cms.testsupport.FilesApiTestSupport;
import com.fwmotion.threescale.cms.testsupport.SectionsApiTestSupport;
import com.fwmotion.threescale.cms.testsupport.TemplatesApiTestSupport;
import io.gatehill.imposter.embedded.MockEngine;
import io.gatehill.imposter.openapi.embedded.OpenApiImposterBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

public class ThreescaleCmsClientImplIntegrationTest {

    MockEngine threescaleAdminImposter;

    ThreescaleCmsClientFactory clientFactory;

    ThreescaleCmsClient cmsClient;

    @BeforeEach
    void setUp() throws Exception {
        Path configDir = Path.of(
            Objects.requireNonNull(ThreescaleCmsClientImplIntegrationTest.class
                .getResource("/imposter/normal")
            ).toURI());

        threescaleAdminImposter = new OpenApiImposterBuilder<>()
            .withConfigurationDir(configDir)
            .startBlocking();

        clientFactory = new ThreescaleCmsClientFactory();
        clientFactory.setBaseUrl(threescaleAdminImposter.getBaseUrl().toString());
        clientFactory.setAccessToken("my access token");

        cmsClient = clientFactory.getThreescaleCmsClient();
    }

    @AfterEach
    void tearDown() {
        try {
            clientFactory.close();
        } catch (Exception e) {
            // Ignore teardown exceptions
        }
    }

    @Test
    void listAllCmsObjects() {
        List<CmsObject> result = cmsClient.listAllCmsObjects();

        assertThat(result, hasSize(97));
        assertThat(result, hasItems(
            FilesApiTestSupport.FAVICON_FILE_MATCHER,
            SectionsApiTestSupport.ROOT_SECTION_MATCHER,
            TemplatesApiTestSupport.MAIN_LAYOUT_MATCHER
        ));
    }
}
