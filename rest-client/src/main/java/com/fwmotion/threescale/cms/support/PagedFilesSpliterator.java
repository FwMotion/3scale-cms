package com.fwmotion.threescale.cms.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fwmotion.threescale.cms.mappers.CmsFileMapper;
import com.fwmotion.threescale.cms.model.CmsFile;
import com.redhat.threescale.rest.cms.ApiException;
import com.redhat.threescale.rest.cms.api.FilesApi;
import com.redhat.threescale.rest.cms.model.FileList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.apache.commons.collections4.ListUtils;
import org.mapstruct.factory.Mappers;

import java.util.*;
import java.util.stream.Collectors;

public class PagedFilesSpliterator extends AbstractPagedRestApiSpliterator<CmsFile> {

    private static final CmsFileMapper FILE_MAPPER = Mappers.getMapper(CmsFileMapper.class);

    private final FilesApi filesApi;

    public PagedFilesSpliterator(@Nonnull FilesApi filesApi,
                                 @Nonnull ObjectMapper objectMapper) {
        super(Collections.emptySet(), objectMapper, 0);
        this.filesApi = filesApi;
    }

    public PagedFilesSpliterator(@Nonnull FilesApi filesApi,
                                 @Nonnull ObjectMapper objectMapper,
                                 @Positive int requestedPageSize) {
        super(requestedPageSize, objectMapper, Collections.emptySet(), 0);
        this.filesApi = filesApi;
    }

    private PagedFilesSpliterator(@Nonnull FilesApi filesApi,
                                  @Nonnull ObjectMapper objectMapper,
                                  @Positive int requestedPageSize,
                                  @Nonnull Collection<CmsFile> currentPage,
                                  @PositiveOrZero int currentPageNumber) {
        super(requestedPageSize, objectMapper, currentPage, currentPageNumber);
        this.filesApi = filesApi;
    }

    @Nullable
    @Override
    protected Collection<CmsFile> getPage(@PositiveOrZero int pageNumber,
                                          @Positive int pageSize) {

        try {
            FileList fileList = filesApi.listFiles(pageNumber, pageSize, null);

            List<CmsFile> resultPage = ListUtils.emptyIfNull(fileList.getCollection())
                .stream()
                .map(FILE_MAPPER::fromRest)
                .collect(Collectors.toList());

            validateResultPageSize(
                "file",
                pageNumber,
                pageSize,
                resultPage,
                fileList.getMetadata());

            return resultPage;
        } catch (ApiException e) {
            throw handleApiException(e, "file", pageNumber, pageSize);
        }
    }

    @Nonnull
    @Override
    protected AbstractPagedRestApiSpliterator<CmsFile> doSplit(
        @Positive int requestedPageSize,
        @Nonnull Collection<CmsFile> currentPage,
        @PositiveOrZero int currentPageNumber) {
        return new PagedFilesSpliterator(filesApi, getObjectMapper(), requestedPageSize, currentPage, currentPageNumber);
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
    public Comparator<? super CmsFile> getComparator() {
        return Comparator.comparing(CmsFile::getId);
    }
}
