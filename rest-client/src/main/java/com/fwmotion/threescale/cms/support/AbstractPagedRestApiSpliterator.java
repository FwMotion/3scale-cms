package com.fwmotion.threescale.cms.support;

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
    private Iterator<T> currentPageIterator;
    private int currentPageSize;
    private int currentPageNumber;
    private boolean didSplit = false;

    protected AbstractPagedRestApiSpliterator(@Positive int requestedPageSize,
                                              @Nonnull Collection<T> currentPage,
                                              @PositiveOrZero int currentPageNumber) {
        this.requestedPageSize = requestedPageSize;
        this.currentPageIterator = currentPage.iterator();
        this.currentPageSize = currentPage.size();
        this.currentPageNumber = currentPageNumber;
    }

    protected AbstractPagedRestApiSpliterator(@Nonnull Collection<T> currentPage,
                                              @PositiveOrZero int currentPageNumber) {
        this(DEFAULT_REQUESTED_PAGE_SIZE, currentPage, currentPageNumber);
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

        // TODO: Create ThreescaleCMSException and throw it instead of IllegalStateException
        throw new IllegalStateException("Unexpected page size for " + type + " list page " + pageNumber
            + " (with page size of " + pageSize
            + "); parsed page size is " + resultPage.size()
            + " but expected size of " + expectedPageSize);
    }

}
