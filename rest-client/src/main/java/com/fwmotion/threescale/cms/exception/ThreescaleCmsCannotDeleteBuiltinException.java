package com.fwmotion.threescale.cms.exception;

import com.redhat.threescale.rest.cms.model.Error;
import jakarta.annotation.Nonnull;
import org.apache.hc.core5.http.HttpStatus;

public class ThreescaleCmsCannotDeleteBuiltinException extends ThreescaleCmsApiException {

    /**
     * The HTTP status code sent by 3scale when this error occurs
     */
    public static final int ERROR_HTTP_CODE = HttpStatus.SC_UNPROCESSABLE_ENTITY;

    /**
     * The error message sent by 3scale to indicate this type of error
     */
    public static final String ERROR_MESSAGE = "Built-in resources can't be deleted";

    public ThreescaleCmsCannotDeleteBuiltinException() {
        super(ERROR_HTTP_CODE,
            new Error()
                .error(ERROR_MESSAGE));
    }
    public ThreescaleCmsCannotDeleteBuiltinException(@Nonnull Error apiError) {
        super(HttpStatus.SC_UNPROCESSABLE_ENTITY,
            apiError);
    }
}
