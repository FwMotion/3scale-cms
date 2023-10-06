package com.fwmotion.threescale.cms;

import com.fwmotion.threescale.cms.model.*;
import com.redhat.threescale.rest.cms.ApiException;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ThreescaleCmsClient {

    // TODO: Replace all "throws ApiException" with better exception handling

    @Nonnull
    default Stream<CmsObject> streamAllCmsObjects() {
        return Stream.of(
            streamSections(),
            streamFiles(),
            streamTemplates()
        ).flatMap(s -> s);
    }

    @Nonnull
    default List<CmsObject> listAllCmsObjects() {
        return streamAllCmsObjects()
            .collect(Collectors.toList());
    }

    @Nonnull
    Stream<CmsSection> streamSections();

    @Nonnull
    default List<CmsSection> listSections() {
        return streamSections()
            .collect(Collectors.toList());
    }

    @Nonnull
    Stream<CmsFile> streamFiles();

    @Nonnull
    default List<CmsFile> listFiles() {
        return streamFiles()
            .collect(Collectors.toList());
    }

    @Nonnull
    Optional<InputStream> getFileContent(long fileId) throws ApiException;

    @Nonnull
    default Optional<InputStream> getFileContent(@Nonnull CmsFile file) {
        return Optional.of(file)
            .map(CmsFile::getId)
            .flatMap(fileId -> {
                try {
                    return getFileContent(fileId);
                } catch (ApiException e) {
                    // TODO: Create ThreescaleCmsException and throw it instead of RuntimeException
                    throw new RuntimeException(e);
                }
            });
    }

    @Nonnull
    Stream<CmsTemplate> streamTemplates();

    @Nonnull
    default List<CmsTemplate> listTemplates() {
        return streamTemplates()
            .collect(Collectors.toList());
    }

    @Nonnull
    Optional<InputStream> getTemplateDraft(long templateId) throws ApiException;

    @Nonnull
    default Optional<InputStream> getTemplateDraft(@Nonnull CmsTemplate template) {
        return Optional.of(template)
            .map(CmsTemplate::getId)
            .flatMap(templateId -> {
                try {
                    return getTemplateDraft(templateId);
                } catch (ApiException e) {
                    // TODO: Create ThreescaleCmsException and throw it instead of RuntimeException
                    throw new RuntimeException(e);
                }
            });
    }

    @Nonnull
    Optional<InputStream> getTemplatePublished(long templateId) throws ApiException;

    @Nonnull
    default Optional<InputStream> getTemplatePublished(@Nonnull CmsTemplate template) {
        return Optional.of(template)
            .map(CmsTemplate::getId)
            .flatMap(templateId -> {
                try {
                    return getTemplatePublished(templateId);
                } catch (ApiException e) {
                    // TODO: Create ThreescaleCmsException and throw it instead of RuntimeException
                    throw new RuntimeException(e);
                }
            });
    }

    void save(@Nonnull CmsSection section) throws ApiException;

    void save(@Nonnull CmsFile file, @Nonnull File fileContent) throws ApiException;

    void save(@Nonnull CmsTemplate template, @Nonnull File draft) throws ApiException;

    void publish(long templateId) throws ApiException;

    default void publish(@Nonnull CmsTemplate template) throws ApiException {
        publish(template.getId());
    }

    void delete(@Nonnull ThreescaleObjectType type, long id) throws ApiException;

    default void delete(@Nonnull CmsObject object) throws ApiException {
        if (object.getId() != null) {
            delete(object.getType(), object.getId());
        }
    }
}
