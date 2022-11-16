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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.mapstruct.factory.Mappers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
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
            // TODO: Create ThreescaleCmsException and throw it instead of ApiException
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
            result = Optional.ofNullable(template.getPublished())
                .map(StringUtils::trimToNull)
                .map(input -> IOUtils.toInputStream(input, Charset.defaultCharset()));
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
            if (StringUtils.isBlank(restSection.getTitle())
                && StringUtils.isNotBlank(restSection.getSystemName())) {
                restSection.setTitle(restSection.getSystemName());
            }

            Section response = sectionsApi.createSection(
                restSection.getPublic(),
                restSection.getTitle(),
                restSection.getParentId(),
                restSection.getPartialPath(),
                restSection.getSystemName());

            section.setId(response.getId());
        } else {
            sectionsApi.updateSection(restSection.getId(),
                restSection.getPublic(),
                restSection.getTitle(),
                restSection.getParentId());
        }
    }

    @Override
    public void save(@Nonnull CmsFile file, @Nullable File fileContent) throws ApiException {
        ModelFile restFile = FILE_MAPPER.toRest(file);

        if (file.getId() == null) {
            ModelFile response = filesApi.createFile(
                restFile.getSectionId(),
                restFile.getPath(),
                fileContent,
                restFile.getTagList(),
                restFile.getDownloadable());

            file.setId(response.getId());
        } else {
            filesApi.updateFile(file.getId(),
                restFile.getSectionId(),
                restFile.getPath(),
                restFile.getTagList(),
                restFile.getDownloadable(),
                fileContent);
        }
    }

    @Override
    public void save(@Nonnull CmsTemplate template, @Nullable File templateDraft) throws ApiException {
        if (template instanceof CmsBuiltinPage) {
            saveBuiltinPage((CmsBuiltinPage) template, templateDraft);
        } else if (template instanceof CmsBuiltinPartial) {
            saveBuiltinPartial((CmsBuiltinPartial) template, templateDraft);
        } else if (template instanceof CmsLayout) {
            saveLayout((CmsLayout) template, templateDraft);
        } else if (template instanceof CmsPage) {
            savePage((CmsPage) template, templateDraft);
        } else if (template instanceof CmsPartial) {
            savePartial((CmsPartial) template, templateDraft);
        }
    }

    private void saveBuiltinPage(@Nonnull CmsBuiltinPage page, @Nullable File templateDraft) throws ApiException {
        if (page.getId() == null) {
            throw new IllegalArgumentException("Built-in pages cannot be created.");
        }

        saveUpdatedTemplate(page.getId(),
            TEMPLATE_MAPPER.toRestBuiltinPage(page),
            templateDraft);
    }

    private void saveBuiltinPartial(@Nonnull CmsBuiltinPartial partial, @Nullable File templateDraft) throws ApiException {
        if (partial.getId() == null) {
            throw new IllegalArgumentException("Built-in partials cannot be created.");
        }

        saveUpdatedTemplate(partial.getId(),
            TEMPLATE_MAPPER.toRestBuiltinPartial(partial),
            templateDraft);
    }

    private void saveLayout(@Nonnull CmsLayout layout, @Nullable File templateDraft) throws ApiException {
        if (layout.getId() == null) {
            Template response = saveNewTemplate(
                TEMPLATE_MAPPER.toRestLayoutCreation(layout),
                templateDraft);

            layout.setId(response.getId());
        } else {
            saveUpdatedTemplate(layout.getId(),
                TEMPLATE_MAPPER.toRestLayoutUpdate(layout),
                templateDraft);
        }
    }

    private void savePage(@Nonnull CmsPage page, @Nullable File templateDraft) throws ApiException {
        if (page.getId() == null) {
            Template response = saveNewTemplate(
                TEMPLATE_MAPPER.toRestPageCreation(page), templateDraft);

            page.setId(response.getId());
        } else {
            saveUpdatedTemplate(page.getId(),
                TEMPLATE_MAPPER.toRestPageUpdate(page),
                templateDraft);
        }
    }

    private void savePartial(@Nonnull CmsPartial partial, @Nullable File templateDraft) throws ApiException {
        if (partial.getId() == null) {
            Template response = saveNewTemplate(
                TEMPLATE_MAPPER.toRestPartialCreation(partial),
                templateDraft);

            partial.setId(response.getId());
        } else {
            saveUpdatedTemplate(partial.getId(),
                TEMPLATE_MAPPER.toRestPartialUpdate(partial),
                templateDraft);
        }
    }

    private Template saveNewTemplate(@Nonnull TemplateCreationRequest template, @Nullable File templateDraft) throws ApiException {
        if (templateDraft == null) {
            throw new IllegalArgumentException("New template must have draft content");
        }

        String draft;
        try {
            draft = FileUtils.readFileToString(templateDraft, Charset.defaultCharset());
        } catch (IOException e) {
            // TODO: Create ThreescaleCmsException and throw it instead of RuntimeException
            throw new RuntimeException(e);
        }

        return templatesApi.createTemplate(template.getType(),
            template.getSystemName(),
            template.getTitle(),
            template.getPath(),
            draft,
            template.getSectionId(),
            template.getLayoutName(),
            template.getLayoutId(),
            template.getLiquidEnabled(),
            template.getHandler(),
            template.getContentType());

    }

    private Template saveUpdatedTemplate(int id, @Nonnull TemplateUpdatableFields template, @Nullable File templateDraft) throws ApiException {

        String draft;
        if (templateDraft == null) {
            draft = null;
        } else {
            try {
                draft = FileUtils.readFileToString(templateDraft, Charset.defaultCharset());
            } catch (IOException e) {
                // TODO: Create ThreescaleCmsException and throw it instead of RuntimeException
                throw new RuntimeException(e);
            }
        }

        return templatesApi.updateTemplate(id,
            template.getSystemName(),
            template.getTitle(),
            template.getPath(),
            draft,
            template.getSectionId(),
            template.getLayoutName(),
            template.getLayoutId(),
            template.getLiquidEnabled(),
            template.getHandler(),
            template.getContentType());
    }

    @Override
    public void publish(@Nonnull CmsTemplate template) throws ApiException {
        templatesApi.publishTemplate(template.getId());
    }

    @Override
    public void delete(@Nonnull ThreescaleObjectType type, int id) throws ApiException {
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
