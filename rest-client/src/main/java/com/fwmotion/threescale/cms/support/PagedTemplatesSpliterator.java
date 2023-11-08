package com.fwmotion.threescale.cms.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fwmotion.threescale.cms.mappers.CmsTemplateMapper;
import com.fwmotion.threescale.cms.model.CmsTemplate;
import com.redhat.threescale.rest.cms.ApiException;
import com.redhat.threescale.rest.cms.api.TemplatesApi;
import com.redhat.threescale.rest.cms.model.TemplateList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.apache.commons.collections4.ListUtils;
import org.mapstruct.factory.Mappers;

import java.util.*;
import java.util.stream.Collectors;

public class PagedTemplatesSpliterator extends AbstractPagedRestApiSpliterator<CmsTemplate> {

    private static final CmsTemplateMapper TEMPLATE_MAPPER = Mappers.getMapper(CmsTemplateMapper.class);

    private final TemplatesApi templatesApi;
    private final boolean includeContent;

    public PagedTemplatesSpliterator(@Nonnull TemplatesApi templatesApi,
                                     @Nonnull ObjectMapper objectMapper,
                                     boolean includeContent) {
        super(Collections.emptySet(), objectMapper, 0);
        this.templatesApi = templatesApi;
        this.includeContent = includeContent;
    }

    public PagedTemplatesSpliterator(@Nonnull TemplatesApi templatesApi,
                                     @Nonnull ObjectMapper objectMapper,
                                     boolean includeContent,
                                     @Positive int requestedPageSize) {
        super(requestedPageSize, objectMapper, Collections.emptySet(), 0);
        this.templatesApi = templatesApi;
        this.includeContent = includeContent;
    }

    private PagedTemplatesSpliterator(@Nonnull TemplatesApi templatesApi,
                                      @Nonnull ObjectMapper objectMapper,
                                      boolean includeContent,
                                      @Positive int requestedPageSize,
                                      @Nonnull Collection<CmsTemplate> currentPage,
                                      @PositiveOrZero int currentPageNumber) {
        super(requestedPageSize, objectMapper, currentPage, currentPageNumber);
        this.templatesApi = templatesApi;
        this.includeContent = includeContent;
    }

    @Nullable
    @Override
    protected Collection<CmsTemplate> getPage(@PositiveOrZero int pageNumber,
                                              @Positive int pageSize) {
        try {
            TemplateList templateList = templatesApi.listTemplates(
                pageNumber,
                pageSize,
                includeContent
            );

            List<CmsTemplate> resultPage = ListUtils
                .emptyIfNull(templateList.getCollection())
                .stream()
                .map(TEMPLATE_MAPPER::fromRest)
                .sorted(getComparator())
                .collect(Collectors.toList());

            validateResultPageSize(
                "template",
                pageNumber,
                pageSize,
                resultPage,
                templateList.getMetadata());

            return resultPage;
        } catch (ApiException e) {
            throw handleApiException(e, "template", pageNumber, pageSize);
        }
    }

    @Nonnull
    @Override
    protected AbstractPagedRestApiSpliterator<CmsTemplate> doSplit(
        @Positive int requestedPageSize,
        @Nonnull Collection<CmsTemplate> currentPage,
        @PositiveOrZero int currentPageNumber
    ) {
        return new PagedTemplatesSpliterator(
            templatesApi,
            getObjectMapper(),
            includeContent,
            requestedPageSize,
            currentPage,
            currentPageNumber
        );
    }

    @Override
    public int characteristics() {
        return Spliterator.DISTINCT |
            Spliterator.SORTED |
            Spliterator.ORDERED |
            Spliterator.NONNULL |
            Spliterator.IMMUTABLE;
    }

    @Nonnull
    @Override
    public Comparator<? super CmsTemplate> getComparator() {
        return Comparator.comparing(CmsTemplate::getId);
    }
}
