package com.fwmotion.threescale.cms.support;

import com.fwmotion.threescale.cms.mappers.CmsTemplateMapper;
import com.fwmotion.threescale.cms.model.CmsTemplate;
import com.redhat.threescale.rest.cms.ApiException;
import com.redhat.threescale.rest.cms.api.TemplatesApi;
import com.redhat.threescale.rest.cms.model.TemplateList;
import org.apache.commons.collections4.ListUtils;
import org.mapstruct.factory.Mappers;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PagedTemplatesSpliterator extends AbstractPagedRestApiSpliterator<CmsTemplate> {

    private static final CmsTemplateMapper TEMPLATE_MAPPER = Mappers.getMapper(CmsTemplateMapper.class);

    private final TemplatesApi templatesApi;

    public PagedTemplatesSpliterator(@Nonnull TemplatesApi templatesApi) {
        super(Collections.emptySet(), 0);
        this.templatesApi = templatesApi;
    }

    public PagedTemplatesSpliterator(@Nonnull TemplatesApi templatesApi,
                                     @Nonnegative int requestedPageSize) {
        super(requestedPageSize, Collections.emptySet(), 0);
        this.templatesApi = templatesApi;
    }

    private PagedTemplatesSpliterator(@Nonnull TemplatesApi templatesApi,
                                      @Nonnegative int requestedPageSize,
                                      @Nonnull Collection<CmsTemplate> currentPage,
                                      @Nonnegative int currentPageNumber) {
        super(requestedPageSize, currentPage, currentPageNumber);
        this.templatesApi = templatesApi;
    }

    @Nullable
    @Override
    protected Collection<CmsTemplate> getPage(@Nonnegative int pageNumber,
                                              @Nonnegative int pageSize) {
        try {
            TemplateList templateList = templatesApi.listTemplates(pageNumber, pageSize);

            List<CmsTemplate> resultPage = Stream.of(
                    ListUtils.emptyIfNull(templateList.getBuiltinPages()).stream()
                        .map(TEMPLATE_MAPPER::fromRestBuiltinPage),
                    ListUtils.emptyIfNull(templateList.getBuiltinPartials()).stream()
                        .map(TEMPLATE_MAPPER::fromRestBuiltinPartial),
                    ListUtils.emptyIfNull(templateList.getLayouts()).stream()
                        .map(TEMPLATE_MAPPER::fromRestLayout),
                    ListUtils.emptyIfNull(templateList.getPages()).stream()
                        .map(TEMPLATE_MAPPER::fromRestPage),
                    ListUtils.emptyIfNull(templateList.getPartials()).stream()
                        .map(TEMPLATE_MAPPER::fromRestPartial)
                ).flatMap(s -> s)
                .sorted(getComparator())
                .collect(Collectors.toList());

            validateResultPageSize(
                "template",
                pageNumber,
                pageSize,
                resultPage,
                templateList::getCurrentPage,
                templateList::getTotalPages,
                templateList::getPerPage,
                templateList::getTotalEntries);

            return resultPage;
        } catch (ApiException e) {
            // TODO: Create ThreescaleCmsException and throw it instead of IllegalStateException
            throw new IllegalStateException("Unexpected exception while iterating CMS template list page " + pageNumber
                + " (with page size of " + pageSize + ")", e);
        }
    }

    @Nonnull
    @Override
    protected AbstractPagedRestApiSpliterator<CmsTemplate> doSplit(
        @Nonnegative int requestedPageSize,
        @Nonnull Collection<CmsTemplate> currentPage,
        @Nonnegative int currentPageNumber) {
        return new PagedTemplatesSpliterator(templatesApi, requestedPageSize, currentPage, currentPageNumber);
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
