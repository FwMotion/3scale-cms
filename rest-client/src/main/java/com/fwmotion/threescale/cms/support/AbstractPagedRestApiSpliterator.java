package com.fwmotion.threescale.cms.support;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class AbstractPagedRestApiSpliterator<T> implements Spliterator<T> {

    protected static final int DEFAULT_REQUESTED_PAGE_SIZE = 20;

    private final int requestedPageSize;
    private Iterator<T> currentPageIterator;
    private int currentPageSize;
    private int currentPageNumber;
    private boolean didSplit = false;

    protected AbstractPagedRestApiSpliterator(@Nonnegative int requestedPageSize,
                                              @Nonnull Collection<T> currentPage,
                                              @Nonnegative int currentPageNumber) {
        this.requestedPageSize = requestedPageSize;
        this.currentPageIterator = currentPage.iterator();
        this.currentPageSize = currentPage.size();
        this.currentPageNumber = currentPageNumber;
    }

    protected AbstractPagedRestApiSpliterator(@Nonnull Collection<T> currentPage,
                                              @Nonnegative int currentPageNumber) {
        this(DEFAULT_REQUESTED_PAGE_SIZE, currentPage, currentPageNumber);
    }

    @Nullable
    abstract protected Collection<T> getPage(@Nonnegative int pageNumber,
                                             @Nonnegative int pageSize);

    @Nonnull
    abstract protected AbstractPagedRestApiSpliterator<T> doSplit(
        @Nonnegative int requestedPageSize,
        @Nonnull Collection<T> currentPage,
        @Nonnegative int currentPageNumber);

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

    @Nonnegative
    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    protected void validateResultPageSize(@Nonnull String type,
                                          @Nonnegative int pageNumber,
                                          @Nonnegative int pageSize,
                                          @Nonnull Collection<T> resultPage,
                                          @Nonnull Supplier<Integer> getCurrentPage,
                                          @Nonnull Supplier<Integer> getTotalPages,
                                          @Nonnull Supplier<Integer> getPerPage,
                                          @Nonnull Supplier<Integer> getTotalEntries) {
        int currentPage = Optional.ofNullable(getCurrentPage.get())
            .orElse(pageNumber);
        int totalPages = Optional.ofNullable(getTotalPages.get())
            .orElse(Integer.MAX_VALUE);
        int perPage = Optional.ofNullable(getPerPage.get())
            .orElse(pageSize);

        int expectedPageSize;
        if (currentPage > totalPages) {
            expectedPageSize = 0;
        } else if (currentPage == totalPages) {
            expectedPageSize = Optional.ofNullable(getTotalEntries.get())
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
