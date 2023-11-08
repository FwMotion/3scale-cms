package com.fwmotion.threescale.cms.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fwmotion.threescale.cms.exception.ThreescaleCmsApiException;
import com.fwmotion.threescale.cms.exception.ThreescaleCmsException;
import com.fwmotion.threescale.cms.exception.ThreescaleCmsUnexpectedPaginationException;
import com.redhat.threescale.rest.cms.ApiException;
import com.redhat.threescale.rest.cms.model.Error;
import com.redhat.threescale.rest.cms.model.ListPaginationMetadata;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.*;
import java.util.function.Consumer;

public abstract class AbstractPagedRestApiSpliterator<T> implements Spliterator<T> {

    protected static final int DEFAULT_REQUESTED_PAGE_SIZE = 20;

    private final int requestedPageSize;
    private final ObjectMapper objectMapper;
    private Iterator<T> currentPageIterator;
    private int currentPageSize;
    private int currentPageNumber;
    private boolean didSplit = false;

    protected AbstractPagedRestApiSpliterator(@Positive int requestedPageSize,
                                              @Nonnull ObjectMapper objectMapper,
                                              @Nonnull Collection<T> currentPage,
                                              @PositiveOrZero int currentPageNumber) {
        this.requestedPageSize = requestedPageSize;
        this.objectMapper = objectMapper;
        this.currentPageIterator = currentPage.iterator();
        this.currentPageSize = currentPage.size();
        this.currentPageNumber = currentPageNumber;
    }

    protected AbstractPagedRestApiSpliterator(@Nonnull Collection<T> currentPage,
                                              @Nonnull ObjectMapper objectMapper,
                                              @PositiveOrZero int currentPageNumber) {
        this(DEFAULT_REQUESTED_PAGE_SIZE, objectMapper, currentPage, currentPageNumber);
    }

    @Nonnull
    ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Nullable
    abstract protected Collection<T> getPage(@PositiveOrZero int pageNumber,
                                             @Positive int pageSize);

    @Nonnull
    abstract protected AbstractPagedRestApiSpliterator<T> doSplit(
        @Positive int requestedPageSize,
        @Nonnull Collection<T> currentPage,
        @PositiveOrZero int currentPageNumber);

    @Nonnull
    @Override
    public abstract Comparator<? super T> getComparator();

    @Override
    public boolean tryAdvance(@Nonnull Consumer<? super T> action) {
        // Try to advance to next page if the current one doesn't exist
        if (!currentPageIterator.hasNext()) {
            if (didSplit) {
                return false;
            }

            Collection<T> nextPage = getPage(currentPageNumber + 1, requestedPageSize);

            if (nextPage == null || nextPage.isEmpty()) {
                didSplit = true;
                return false;
            }

            currentPageIterator = nextPage.iterator();
            currentPageSize = nextPage.size();
            currentPageNumber++;
        }

        action.accept(currentPageIterator.next());
        return true;
    }

    @Nullable
    @Override
    public AbstractPagedRestApiSpliterator<T> trySplit() {
        // Don't split again
        if (didSplit) {
            return null;
        }
        didSplit = true;

        // If at the end of the list, return null
        if (currentPageNumber > 0 && !currentPageIterator.hasNext() && currentPageSize < requestedPageSize) {
            return null;
        }

        // Get the next page
        Collection<T> nextPage = getPage(currentPageNumber + 1, requestedPageSize);

        if (nextPage == null || nextPage.isEmpty()) {
            return null;
        }

        return doSplit(requestedPageSize, nextPage, currentPageNumber + 1);
    }

    @PositiveOrZero
    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    protected void validateResultPageSize(@Nonnull String type,
                                          @PositiveOrZero int pageNumber,
                                          @Positive int pageSize,
                                          @Nonnull Collection<T> resultPage,
                                          @Nullable ListPaginationMetadata paginationMetadata) {
        int currentPage = Optional.ofNullable(paginationMetadata)
            .map(ListPaginationMetadata::getCurrentPage)
            .orElse(pageNumber);
        Optional<Integer> totalPagesOptional = Optional.ofNullable(paginationMetadata)
            .map(ListPaginationMetadata::getTotalPages);
        int totalPages = totalPagesOptional
            .orElse(Integer.MAX_VALUE);
        int perPage = Optional.ofNullable(paginationMetadata)
            .map(ListPaginationMetadata::getPerPage)
            .orElse(pageSize);

        int expectedPageSize;
        if (currentPage > totalPages) {
            expectedPageSize = 0;
        } else if (totalPagesOptional.isEmpty()
            || currentPage == totalPages) {
            expectedPageSize = Optional.ofNullable(paginationMetadata)
                .map(ListPaginationMetadata::getTotalEntries)
                .map(totalEntries -> totalEntries % perPage)
                .orElseGet(resultPage::size);
        } else {
            expectedPageSize = perPage;
        }

        if (resultPage.size() == expectedPageSize) {
            return;
        }

        throw new ThreescaleCmsUnexpectedPaginationException(type,
            pageNumber,
            pageSize,
            resultPage.size(),
            expectedPageSize);
    }

    protected ThreescaleCmsException handleApiException(
        @Nonnull ApiException e,
        @Nonnull String type,
        @PositiveOrZero int pageNumber,
        @Positive int pageSize
    ) {
        String errorMessage = "Unexpected exception while iterating CMS " + type
            + " page " + pageNumber
            + " (with page size of " + pageSize + ")";

        Error responseError;
        try {
            responseError = objectMapper.readValue(e.getResponseBody(), Error.class);
        } catch (JsonProcessingException ex) {
            return new ThreescaleCmsApiException(e.getCode(), errorMessage, e);
        }

        return new ThreescaleCmsApiException(e.getCode(),
            responseError,
            errorMessage,
            e);
    }

}
