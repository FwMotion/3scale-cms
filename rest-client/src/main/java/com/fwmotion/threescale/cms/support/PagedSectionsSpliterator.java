package com.fwmotion.threescale.cms.support;

import com.fwmotion.threescale.cms.mappers.CmsBuiltinSectionMapper;
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
import java.util.stream.Stream;

public class PagedSectionsSpliterator extends AbstractPagedRestApiSpliterator<CmsSection> {

    private static final CmsSectionMapper SECTION_MAPPER = Mappers.getMapper(CmsSectionMapper.class);
    private static final CmsBuiltinSectionMapper BUILTIN_SECTION_MAPPER = Mappers.getMapper(CmsBuiltinSectionMapper.class);

    private final SectionsApi sectionsApi;

    public PagedSectionsSpliterator(@Nonnull SectionsApi sectionsApi) {
        super(Collections.emptySet(), 0);
        this.sectionsApi = sectionsApi;
    }

    public PagedSectionsSpliterator(@Nonnull SectionsApi sectionsApi,
                                    @Positive int requestedPageSize) {
        super(requestedPageSize, Collections.emptySet(), 0);
        this.sectionsApi = sectionsApi;
    }

    private PagedSectionsSpliterator(@Nonnull SectionsApi sectionsApi,
                                     @Positive int requestedPageSize,
                                     @Nonnull Collection<CmsSection> currentPage,
                                     @PositiveOrZero int currentPageNumber) {
        super(requestedPageSize, currentPage, currentPageNumber);
        this.sectionsApi = sectionsApi;
    }

    @Nullable
    @Override
    protected Collection<CmsSection> getPage(@PositiveOrZero int pageNumber,
                                             @Positive int pageSize) {
        try {
            SectionList sectionList = sectionsApi.listSections(pageNumber, pageSize);

            List<CmsSection> resultPage =
                Stream.concat(
                    ListUtils.emptyIfNull(sectionList.getBuiltinSections())
                        .stream()
                        .map(BUILTIN_SECTION_MAPPER::fromRest),
                    ListUtils.emptyIfNull(sectionList.getSections())
                        .stream()
                        .map(SECTION_MAPPER::fromRest)
                ).collect(Collectors.toList());

            validateResultPageSize(
                "section",
                pageNumber,
                pageSize,
                resultPage,
                sectionList::getCurrentPage,
                sectionList::getTotalPages,
                sectionList::getPerPage,
                sectionList::getTotalEntries);

            return resultPage;
        } catch (ApiException e) {
            // TODO: Create ThreescaleCmsException and throw it instead of IllegalStateException
            throw new IllegalStateException("Unexpected exception while iterating CMS section list page " + pageNumber
                + " (with page size of " + pageSize + ")", e);
        }
    }

    @Nonnull
    @Override
    protected AbstractPagedRestApiSpliterator<CmsSection> doSplit(
        @Positive int requestedPageSize,
        @Nonnull Collection<CmsSection> currentPage,
        @PositiveOrZero int currentPageNumber) {
        return new PagedSectionsSpliterator(sectionsApi, requestedPageSize, currentPage, currentPageNumber);
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
