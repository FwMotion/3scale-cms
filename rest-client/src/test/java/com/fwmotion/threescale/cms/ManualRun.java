package com.fwmotion.threescale.cms;


import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Quick main class for testing outputs prior to building CLI and real tests.
 * <p>
 * TODO: Replace this with real tests
 */
public class ManualRun {

    public static void main(String[] args) throws Exception {
        try (ThreescaleCmsClientFactory factory = new ThreescaleCmsClientFactory()) {

            factory.setBaseUrl(System.getenv("THREESCALE_BASE_URL"));
            factory.setProviderKey(System.getenv("THREESCALE_PROVIDER_KEY"));
            factory.setUseInsecureConnections(true);

            ThreescaleCmsClient client = factory.getThreescaleCmsClient();

            client.streamTemplates()
                .forEach(template -> {
                    System.out.println(template);
                    client.getTemplateDraft(template)
                        .map(content -> {
                            try {
                                return IOUtils.toString(content, Charset.defaultCharset());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .ifPresent(System.out::println);
                });
        }
    }
}
