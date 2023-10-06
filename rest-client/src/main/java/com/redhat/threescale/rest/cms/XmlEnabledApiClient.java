package com.redhat.threescale.rest.cms;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The default {@link ApiClient} that's generated by openapi-generator doesn't
 * properly support XML when using the java library's apache-httpclient
 * template. To facilitate use of mixed XML and JSON, this class extends
 * {@link ApiClient} to add XML support where needed.
 */
public class XmlEnabledApiClient extends ApiClient {

    public static final Pattern XML_MIME_PATTERN = Pattern.compile("(?i)^((?:application|text)/xml|[^;/ \t]+/[^;/ \t]+[+]xml)[ \t]*(;.*)?$");

    private final XmlMapper xmlMapper;

    public XmlEnabledApiClient(CloseableHttpClient httpClient) {
        super(httpClient);

        xmlMapper = XmlMapper.builder()
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
            .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
            .addModule(new JavaTimeModule())
            .defaultDateFormat(ApiClient.buildDefaultDateFormat())
            .defaultUseWrapper(false)
            .build();
    }

    public XmlMapper getXmlMapper() {
        return xmlMapper;
    }

    public boolean isXmlMime(String mime) {
        return mime != null && XML_MIME_PATTERN.matcher(mime).matches();
    }

    @Override
    public <T> T deserialize(CloseableHttpResponse response, TypeReference<T> valueType) throws ApiException, IOException, ParseException {
        if (valueType == null) {
            return null;
        }

        Type valueRawType = valueType.getType();
        if (byte[].class.equals(valueRawType) ||
            File.class.equals(valueRawType)) {
            return super.deserialize(response, valueType);
        }

        boolean isXml = false;
        HttpEntity entity = response.getEntity();

        String contentTypeHeader = entity.getContentType();
        ContentType contentType;
        if (contentTypeHeader != null) {
            try {
                contentType = ContentType.parse(contentTypeHeader);
            } catch (Exception e) {
                // Problem parsing; pass it through to the superclass's method
                // (which may just throw an exception itself too)
                return super.deserialize(response, valueType);
            }

            if (this.isXmlMime(contentType.getMimeType())) {
                isXml = true;
            }
        } else {
            contentType = null;
        }

        if (String.class.equals(valueRawType)) {
            //noinspection unchecked
            return (T) IOUtils.toString(entity.getContent(),
                Optional.ofNullable(contentType)
                    .map(ContentType::getCharset)
                    .orElse(Charset.defaultCharset()));
        }

        if (isXml) {
            return xmlMapper.readValue(entity.getContent(), valueType);
        }

        return super.deserialize(response, valueType);
    }
}
