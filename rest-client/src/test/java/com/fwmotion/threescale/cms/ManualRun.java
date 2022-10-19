package com.fwmotion.threescale.cms;


import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;

/**
 * Quick main class for testing outputs prior to building CLI and real tests.
 * <p>
 * TODO: Replace this with real tests
 */
public class ManualRun {

    public static void main(String[] args) throws Exception {
        try (ThreescaleCmsClientFactory factory = new ThreescaleCmsClientFactory()) {

            factory.setBaseUrl(System.getenv("THREESCALE_BASE_URL"));
            Optional.ofNullable(System.getenv("THREESCALE_ACCESS_TOKEN"))
                    .ifPresent(factory::setAccessToken);
            Optional.ofNullable(System.getenv("THREESCALE_PROVIDER_KEY"))
                    .ifPresent(factory::setProviderKey);
            factory.setUseInsecureConnections(true);

            ThreescaleCmsClient client = factory.getThreescaleCmsClient();

            client.streamTemplates()
                .sequential()
                .forEach(template -> {
                    System.out.println("--- template #" + template.getId() + " ---");
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

            try (InputStream fileStream = client.getFileContent(9)
                .orElseThrow(() -> new IllegalStateException("Couldn't read file"))) {
                String fileContent = IOUtils.toString(fileStream, Charset.defaultCharset());

                System.out.println("File size: " + fileContent.length());
            }
        }
    }
}
