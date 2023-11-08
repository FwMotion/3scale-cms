package com.fwmotion.threescale.cms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fwmotion.threescale.cms.exception.*;
import com.fwmotion.threescale.cms.mappers.CmsFileMapper;
import com.fwmotion.threescale.cms.mappers.CmsSectionMapper;
import com.fwmotion.threescale.cms.mappers.CmsTemplateMapper;
import com.fwmotion.threescale.cms.model.*;
import com.fwmotion.threescale.cms.support.PagedFilesSpliterator;
import com.fwmotion.threescale.cms.support.PagedSectionsSpliterator;
import com.fwmotion.threescale.cms.support.PagedTemplatesSpliterator;
import com.redhat.threescale.rest.cms.ApiClient;
import com.redhat.threescale.rest.cms.ApiException;
import com.redhat.threescale.rest.cms.api.FilesApi;
import com.redhat.threescale.rest.cms.api.SectionsApi;
import com.redhat.threescale.rest.cms.api.TemplatesApi;
import com.redhat.threescale.rest.cms.model.Error;
import com.redhat.threescale.rest.cms.model.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.mapstruct.factory.Mappers;

import java.io.ByteArrayInputStream;
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
    private final ObjectMapper objectMapper;

    public ThreescaleCmsClientImpl(@Nonnull FilesApi filesApi,
                                   @Nonnull SectionsApi sectionsApi,
                                   @Nonnull TemplatesApi templatesApi,
                                   @Nonnull ObjectMapper objectMapper) {
        this.filesApi = filesApi;
        this.sectionsApi = sectionsApi;
        this.templatesApi = templatesApi;
        this.objectMapper = objectMapper;
    }

    public ThreescaleCmsClientImpl(@Nonnull ApiClient apiClient) {
        this(new FilesApi(apiClient),
            new SectionsApi(apiClient),
            new TemplatesApi(apiClient),
            apiClient.getObjectMapper());
    }

    private <T> T handleApiErrors(
        @Nonnull ApiBlock<T> apiBlock,
        @Nullable ApiExceptionTransformer<?> exceptionTransformer
    ) throws ThreescaleCmsApiException {
        try {
            return apiBlock.callApi();
        } catch (ApiException e) {
            throw handleApiException(exceptionTransformer, e);
        }
    }

    private <T> T handleApiErrors(@Nonnull ApiBlock<T> apiBlock) throws ThreescaleCmsException {
        return handleApiErrors(apiBlock, null);
    }

    private void handleApiErrors(
        @Nonnull VoidApiBlock apiBlock,
        @Nullable ApiExceptionTransformer<?> exceptionTransformer
    ) throws ThreescaleCmsException {
        try {
            apiBlock.callApi();
        } catch (ApiException e) {
            throw handleApiException(exceptionTransformer, e);
        }
    }

    private void handleApiErrors(@Nonnull VoidApiBlock apiBlock) throws ThreescaleCmsException {
        handleApiErrors(apiBlock, null);
    }

    @Nonnull
    private ThreescaleCmsApiException handleApiException(ApiExceptionTransformer<?> exceptionTransformer, ApiException e) {
        int httpStatus = e.getCode();

        // Try to deserialize a REST-modeled Error object type
        Error responseError;

        try {
            responseError = objectMapper.readValue(e.getResponseBody(), Error.class);
        } catch (JsonProcessingException jsonProcessingException) {
            // If the response body is not parseable into an Error, throw
            // the response body as-is.
            return new ThreescaleCmsApiException(httpStatus, "Unknown ApiException", e);
        }

        ThreescaleCmsApiException apiException = new ThreescaleCmsApiException(
            httpStatus,
            responseError,
            e);

        if (exceptionTransformer == null) {
            return apiException;
        }

        return exceptionTransformer.transformException(apiException);
    }

    @Nonnull
    @Override
    public Stream<CmsSection> streamSections() {
        return StreamSupport.stream(new PagedSectionsSpliterator(sectionsApi, objectMapper), true);
    }

    @Nonnull
    @Override
    public Stream<CmsFile> streamFiles() {
        return StreamSupport.stream(new PagedFilesSpliterator(filesApi, objectMapper), true);
    }

    @Nonnull
    @Override
    public Optional<InputStream> getFileContent(long fileId) {
        return handleApiErrors(() -> {
            CloseableHttpClient httpClient = filesApi.getApiClient().getHttpClient();
            ModelFile file = filesApi.getFile(fileId);
            ProviderAccount account = filesApi.readProviderSettings().getAccount();

            HttpGet request = new HttpGet(account.getBaseUrl() + file.getPath());
            request.setHeader(HttpHeaders.ACCEPT, "*/*");
            if (StringUtils.isNotEmpty(account.getSiteAccessCode())) {
                request.addHeader("Cookie", "access_code=" + account.getSiteAccessCode());
            }

            try {
                return httpClient.execute(request, response -> {
                    if (response == null) {
                        return Optional.empty();
                    }

                    // TODO: Validate response headers, status code, etc

                    HttpEntity entity = response.getEntity();
                    if (entity == null) {
                        return Optional.empty();
                    }

                    return Optional.of(
                        new ByteArrayInputStream(entity.getContent().readAllBytes())
                    );
                });
            } catch (IOException e) {
                throw new ThreescaleCmsNonApiException("IOException while retrieving file content", e);
            }
        });
    }

    @Nonnull
    @Override
    public Stream<CmsTemplate> streamTemplates(boolean includeContent) {
        return StreamSupport.stream(new PagedTemplatesSpliterator(templatesApi, objectMapper, includeContent), true);
    }

    @Nonnull
    @Override
    public Optional<InputStream> getTemplateDraft(long templateId) {
        return handleApiErrors(() -> {
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
        });
    }

    @Nonnull
    @Override
    public Optional<InputStream> getTemplatePublished(long templateId) {
        return handleApiErrors(() -> {
            Template template = templatesApi.getTemplate(templateId);

            return Optional.ofNullable(template.getPublished())
                .map(input -> IOUtils.toInputStream(input, Charset.defaultCharset()));
        });
    }

    @Override
    public void save(@Nonnull CmsSection section) {
        handleApiErrors(() -> {
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
        });
    }

    @Override
    public void save(@Nonnull CmsFile file, @Nullable File fileContent) {
        handleApiErrors(() -> {
            ModelFile restFile = FILE_MAPPER.toRest(file);

            if (file.getId() == null) {
                ModelFile response = filesApi.createFile(
                    restFile.getSectionId(),
                    restFile.getPath(),
                    fileContent,
                    restFile.getDownloadable(),
                    restFile.getContentType());

                file.setId(response.getId());
            } else {
                filesApi.updateFile(file.getId(),
                    restFile.getSectionId(),
                    restFile.getPath(),
                    restFile.getDownloadable(),
                    fileContent,
                    restFile.getContentType());
            }
        });
    }

    @Override
    public void save(@Nonnull CmsTemplate template, @Nullable File templateDraft) {
        /* When upgraded to JDK21:
        switch (template) {
            case CmsBuiltinPage cmsBuiltinPage -> saveBuiltinPage(cmsBuiltinPage, templateDraft);
            case CmsBuiltinPartial cmsBuiltinPartial -> saveBuiltinPartial(cmsBuiltinPartial, templateDraft);
            case CmsLayout cmsLayout -> saveLayout(cmsLayout, templateDraft);
            case CmsPage cmsPage -> savePage(cmsPage, templateDraft);
            case CmsPartial cmsPartial -> savePartial(cmsPartial, templateDraft);
            default -> throw new UnsupportedOperationException("Unknown template type: " + template.getClass().getName());
        }
        */
        if (template instanceof CmsBuiltinPage cmsBuiltinPage) {
            saveBuiltinPage(cmsBuiltinPage, templateDraft);
        } else if (template instanceof CmsBuiltinPartial cmsBuiltinPartial) {
            saveBuiltinPartial(cmsBuiltinPartial, templateDraft);
        } else if (template instanceof CmsLayout cmsLayout) {
            saveLayout(cmsLayout, templateDraft);
        } else if (template instanceof CmsPage cmsPage) {
            savePage(cmsPage, templateDraft);
        } else if (template instanceof CmsPartial cmsPartial) {
            savePartial(cmsPartial, templateDraft);
        } else {
            throw new UnsupportedOperationException("Unknown template type: " + template.getClass().getName());
        }
    }

    private void saveBuiltinPage(@Nonnull CmsBuiltinPage page, @Nullable File templateDraft) {
        if (page.getId() == null) {
            throw new ThreescaleCmsCannotCreateBuiltinException("Built-in pages can't be created.");
        }

        saveUpdatedTemplate(page.getId(),
            TEMPLATE_MAPPER.toRestBuiltinPage(page),
            templateDraft);
    }

    private void saveBuiltinPartial(@Nonnull CmsBuiltinPartial partial, @Nullable File templateDraft) {
        if (partial.getId() == null) {
            throw new ThreescaleCmsCannotCreateBuiltinException("Built-in partials cannot be created.");
        }

        saveUpdatedTemplate(partial.getId(),
            TEMPLATE_MAPPER.toRestBuiltinPartial(partial),
            templateDraft);
    }

    private void saveLayout(@Nonnull CmsLayout layout, @Nullable File templateDraft) {
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

    private void savePage(@Nonnull CmsPage page, @Nullable File templateDraft) {
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

    private void savePartial(@Nonnull CmsPartial partial, @Nullable File templateDraft) {
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

    private Template saveNewTemplate(@Nonnull TemplateCreationRequest template, @Nullable File templateDraft) {
        if (templateDraft == null) {
            throw new IllegalArgumentException("New template must have draft content");
        }

        String draft;
        try {
            draft = FileUtils.readFileToString(templateDraft, Charset.defaultCharset());
        } catch (IOException e) {
            throw new ThreescaleCmsNonApiException("Exception while reading file content for template draft", e);
        }

        return handleApiErrors(() -> templatesApi.createTemplate(template.getType(),
            template.getSystemName(),
            template.getTitle(),
            template.getPath(),
            draft,
            template.getSectionId(),
            template.getLayoutName(),
            template.getLayoutId(),
            template.getLiquidEnabled(),
            template.getHandler(),
            template.getContentType()));
    }

    @SuppressWarnings("UnusedReturnValue")
    private Template saveUpdatedTemplate(long id, @Nonnull TemplateUpdatableFields template, @Nullable File templateDraft) {
        String draft;
        if (templateDraft == null) {
            draft = null;
        } else {
            try {
                draft = FileUtils.readFileToString(templateDraft, Charset.defaultCharset());
            } catch (IOException e) {
                throw new ThreescaleCmsNonApiException("Exception while reading file content for template draft", e);
            }
        }

        return handleApiErrors(() -> templatesApi.updateTemplate(id,
            template.getSystemName(),
            template.getTitle(),
            template.getPath(),
            draft,
            template.getSectionId(),
            template.getLayoutName(),
            template.getLayoutId(),
            template.getLiquidEnabled(),
            template.getHandler(),
            template.getContentType()));
    }

    @Override
    public void publish(long templateId) throws ThreescaleCmsApiException {
        handleApiErrors(() -> templatesApi.publishTemplate(templateId));
    }

    @Override
    public void delete(@Nonnull ThreescaleObjectType type, long id) throws ThreescaleCmsApiException {
        handleApiErrors(
            () -> {
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
            },
            apiException -> {
                if (apiException.getHttpStatus() == ThreescaleCmsCannotDeleteBuiltinException.ERROR_HTTP_CODE
                    && apiException.getApiError()
                    .filter(apiError -> ThreescaleCmsCannotDeleteBuiltinException.ERROR_MESSAGE.equals(apiError.getError()))
                    .isPresent()
                ) {
                    return new ThreescaleCmsCannotDeleteBuiltinException(
                        apiException.getApiError().get()
                    );
                }

                return apiException;
            }
        );
    }

    @FunctionalInterface
    private interface VoidApiBlock {
        void callApi() throws ApiException;
    }

    @FunctionalInterface
    private interface ApiBlock<T> {
        T callApi() throws ApiException;
    }

    @FunctionalInterface
    private interface ApiExceptionTransformer<T extends ThreescaleCmsApiException> {
        T transformException(ThreescaleCmsApiException e);
    }

}
