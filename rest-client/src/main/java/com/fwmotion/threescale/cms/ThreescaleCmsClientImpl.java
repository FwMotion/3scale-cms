package com.fwmotion.threescale.cms;

import com.fwmotion.threescale.cms.mappers.CmsFileMapper;
import com.fwmotion.threescale.cms.mappers.CmsSectionMapper;
import com.fwmotion.threescale.cms.mappers.CmsTemplateMapper;
import com.fwmotion.threescale.cms.model.*;
import com.fwmotion.threescale.cms.support.PagedFilesSpliterator;
import com.fwmotion.threescale.cms.support.PagedSectionsSpliterator;
import com.fwmotion.threescale.cms.support.PagedTemplatesSpliterator;
import com.redhat.threescale.rest.cms.ApiException;
import com.redhat.threescale.rest.cms.XmlEnabledApiClient;
import com.redhat.threescale.rest.cms.api.FilesApi;
import com.redhat.threescale.rest.cms.api.SectionsApi;
import com.redhat.threescale.rest.cms.api.TemplatesApi;
import com.redhat.threescale.rest.cms.model.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.mapstruct.factory.Mappers;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ThreescaleCmsClientImpl implements ThreescaleCmsClient {

    private static final CmsFileMapper FILE_MAPPER = Mappers.getMapper(CmsFileMapper.class);
    private static final CmsSectionMapper SECTION_MAPPER = Mappers.getMapper(CmsSectionMapper.class);
    private static final CmsTemplateMapper TEMPLATE_MAPPER = Mappers.getMapper(CmsTemplateMapper.class);

    private final FilesApi filesApi;
    private final SectionsApi sectionsApi;
    private final TemplatesApi templatesApi;

    public ThreescaleCmsClientImpl(@Nonnull FilesApi filesApi,
                                   @Nonnull SectionsApi sectionsApi,
                                   @Nonnull TemplatesApi templatesApi) {
        this.filesApi = filesApi;
        this.sectionsApi = sectionsApi;
        this.templatesApi = templatesApi;
    }

    public ThreescaleCmsClientImpl(@Nonnull XmlEnabledApiClient apiClient) {
        this(new FilesApi(apiClient),
            new SectionsApi(apiClient),
            new TemplatesApi(apiClient));
    }

    @Nonnull
    @Override
    public Stream<CmsSection> streamSections() {
        return StreamSupport.stream(new PagedSectionsSpliterator(sectionsApi), true);
    }

    @Nonnull
    @Override
    public Stream<CmsFile> streamFiles() {
        return StreamSupport.stream(new PagedFilesSpliterator(filesApi), true);
    }

    @Nonnull
    @Override
    public Optional<InputStream> getFileContent(int fileId) throws ApiException {
        HttpClient httpClient = filesApi.getApiClient().getHttpClient();
        ModelFile file = filesApi.getFile(fileId);
        ProviderAccount account = filesApi.readProviderSettings();

        HttpGet request = new HttpGet(account.getBaseUrl() + file.getPath());
        request.setHeader(HttpHeaders.ACCEPT, "*/*");
        if (StringUtils.isNotEmpty(account.getSiteAccessCode())) {
            request.addHeader("Cookie", "access_code=" + account.getSiteAccessCode());
        }

        try {
            HttpResponse response = httpClient.execute(request);

            // TODO: Validate response headers, status code, etc

            return Optional.of(response.getEntity().getContent());
        } catch (IOException e) {
            throw new ApiException(e);
        }
    }

    @Nonnull
    @Override
    public Stream<CmsTemplate> streamTemplates() {
        return StreamSupport.stream(new PagedTemplatesSpliterator(templatesApi), true);
    }

    @Nonnull
    @Override
    public Optional<InputStream> getTemplateDraft(int templateId) throws ApiException {
        Template template = templatesApi.getTemplate(templateId);

        Optional<InputStream> result = Optional.ofNullable(template.getDraft())
            .map(StringUtils::trimToNull)
            .map(input -> IOUtils.toInputStream(input, Charset.defaultCharset()));

        // When there's no draft content, the "draft" should be the same as
        // the "published" content
        if (result.isEmpty()) {
            return getTemplatePublished(templateId);
        }

        return result;
    }

    @Nonnull
    @Override
    public Optional<InputStream> getTemplatePublished(int templateId) throws ApiException {
        Template template = templatesApi.getTemplate(templateId);

        return Optional.ofNullable(template.getPublished())
            .map(input -> IOUtils.toInputStream(input, Charset.defaultCharset()));
    }

    @Override
    public void save(@Nonnull CmsSection section) throws ApiException {
        Section restSection = SECTION_MAPPER.toRest(section);
        if (section.getId() == null) {
            // TODO: Add createSection to OpenAPI spec
        } else {
            // TODO: Add update parameters
            sectionsApi.updateSection(restSection.getId());
        }
    }

    @Override
    public void save(@Nonnull CmsFile file, @Nonnull InputStream fileContent) throws ApiException {
        ModelFile restFile = FILE_MAPPER.toRest(file);
        if (file.getId() == null) {
            filesApi.createFile(
                restFile.getSectionId(),
                restFile.getPath(),
                restFile.getTagList(),
                restFile.getDownloadable(),
                // TODO: Send data as "attachment" here
                null);
        } else {
            filesApi.updateFile(file.getId(),
                restFile.getSectionId(),
                restFile.getPath(),
                restFile.getTagList(),
                restFile.getDownloadable(),
                // TODO: Send data as "attachment" here
                null);
        }
    }

    @Override
    public void save(@Nonnull CmsTemplate template, @Nonnull InputStream templateDraft) throws ApiException {
        // TODO: Does saving over builtins do something? should it be supported?
        // TODO: do something with template draft
        if (template instanceof CmsLayout) {
            saveLayout(TEMPLATE_MAPPER.toRestLayout((CmsLayout) template));
        } else if (template instanceof CmsPage) {
            savePage(TEMPLATE_MAPPER.toRestPage((CmsPage) template));
        } else if (template instanceof CmsPartial) {
            savePartial(TEMPLATE_MAPPER.toRestPartial((CmsPartial) template));
        }
    }

    private void saveLayout(Layout layout) throws ApiException {
        if (layout.getId() == null) {
            // TODO: Add create to OpenAPI spec
        } else {
            // TODO: Add update parameters to OpenAPI spec
            templatesApi.updateTemplate(layout.getId());
        }
    }

    private void savePage(Page page) throws ApiException {
        if (page.getId() == null) {
            // TODO: Add create to OpenAPI spec
        } else {
            // TODO: Add update parameters to OpenAPI spec
            templatesApi.updateTemplate(page.getId());
        }
    }

    private void savePartial(Partial partial) throws ApiException {
        if (partial.getId() == null) {
            // TODO: Add create to OpenAPI spec
        } else {
            // TODO: Add update parameters to OpenAPI spec
            templatesApi.updateTemplate(partial.getId());
        }
    }

    @Override
    public void publish(@Nonnull CmsTemplate template) throws ApiException {
        templatesApi.publishTemplate(template.getId());
    }

    @Override
    public void delete(@Nonnull ThreescaleObjectType type, int id) throws
        ApiException {
        switch (type) {
            case SECTION:
                sectionsApi.deleteSection(id);
                break;
            case FILE:
                filesApi.deleteFile(id);
                break;
            case TEMPLATE:
                templatesApi.deleteTemplate(id);
                break;
            default:
                throw new UnsupportedOperationException("Unknown type: " + type);
        }
    }

}
