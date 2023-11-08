package com.fwmotion.threescale.cms.exception;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.apache.hc.core5.http.HttpStatus;

public class ThreescaleCmsUnexpectedPaginationException extends ThreescaleCmsApiException {

    public ThreescaleCmsUnexpectedPaginationException(
        @Nonnull String type,
        @PositiveOrZero int pageNumber,
        @Positive int requestedPageSize,
        @Positive int expectedPageSize,
        @Positive int actualPageSize
    ) {
        super(HttpStatus.SC_OK,
            "Unexpected page size for " + type + " list page " + pageNumber
                + " (with page size of " + requestedPageSize
                + "); parsed page size is " + actualPageSize
                + " but expected size of " + expectedPageSize);
    }
}
