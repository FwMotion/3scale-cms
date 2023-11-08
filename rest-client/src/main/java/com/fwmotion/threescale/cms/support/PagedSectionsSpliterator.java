package com.fwmotion.threescale.cms.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fwmotion.threescale.cms.mappers.CmsSectionMapper;
import com.fwmotion.threescale.cms.model.CmsSection;
import com.redhat.threescale.rest.cms.ApiException;
import com.redhat.threescale.rest.cms.api.SectionsApi;
import com.redhat.threescale.rest.cms.model.SectionList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.apache.commons.collections4.ListUtils;
import org.mapstruct.factory.Mappers;

import java.util.*;
import java.util.stream.Collectors;

public class PagedSectionsSpliterator extends AbstractPagedRestApiSpliterator<CmsSection> {

    private static final CmsSectionMapper SECTION_MAPPER = Mappers.getMapper(CmsSectionMapper.class);

    private final SectionsApi sectionsApi;

    public PagedSectionsSpliterator(@Nonnull SectionsApi sectionsApi, @Nonnull ObjectMapper objectMapper) {
        super(Collections.emptySet(), objectMapper, 0);
        this.sectionsApi = sectionsApi;
    }

    public PagedSectionsSpliterator(@Nonnull SectionsApi sectionsApi,
                                    @Nonnull ObjectMapper objectMapper,
                                    @Positive int requestedPageSize) {
        super(requestedPageSize, objectMapper, Collections.emptySet(), 0);
        this.sectionsApi = sectionsApi;
    }

    private PagedSectionsSpliterator(@Nonnull SectionsApi sectionsApi,
                                     @Nonnull ObjectMapper objectMapper,
                                     @Positive int requestedPageSize,
                                     @Nonnull Collection<CmsSection> currentPage,
                                     @PositiveOrZero int currentPageNumber) {
        super(requestedPageSize, objectMapper, currentPage, currentPageNumber);
        this.sectionsApi = sectionsApi;
    }

    @Nullable
    @Override
    protected Collection<CmsSection> getPage(@PositiveOrZero int pageNumber,
                                             @Positive int pageSize) {
        try {
            SectionList sectionList = sectionsApi.listSections(pageNumber, pageSize);

            List<CmsSection> resultPage =
                ListUtils.emptyIfNull(sectionList.getCollection())
                    .stream()
                    .map(SECTION_MAPPER::fromRest)
                    .collect(Collectors.toList());

            validateResultPageSize(
                "section",
                pageNumber,
                pageSize,
                resultPage,
                sectionList.getMetadata());

            return resultPage;
        } catch (ApiException e) {
            throw handleApiException(e, "section", pageNumber, pageSize);
        }
    }

    @Nonnull
    @Override
    protected AbstractPagedRestApiSpliterator<CmsSection> doSplit(
        @Positive int requestedPageSize,
        @Nonnull Collection<CmsSection> currentPage,
        @PositiveOrZero int currentPageNumber) {
        return new PagedSectionsSpliterator(sectionsApi, getObjectMapper(), requestedPageSize, currentPage, currentPageNumber);
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
    public Comparator<? super CmsSection> getComparator() {
        return Comparator.comparing(CmsSection::getId);
    }

}
