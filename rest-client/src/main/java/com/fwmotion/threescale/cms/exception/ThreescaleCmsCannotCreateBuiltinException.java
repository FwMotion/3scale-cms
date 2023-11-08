package com.fwmotion.threescale.cms.exception;

import jakarta.annotation.Nonnull;
import org.apache.hc.core5.http.HttpStatus;

public class ThreescaleCmsCannotCreateBuiltinException extends ThreescaleCmsApiException {

    public static final int ERROR_HTTP_STATUS = HttpStatus.SC_BAD_REQUEST;

    public ThreescaleCmsCannotCreateBuiltinException(@Nonnull String message) {
        super(ERROR_HTTP_STATUS, message);
    }

}
