package com.fwmotion.threescale.cms.exception;

/**
 * Base class for exceptions not directly related to the CMS API, such as
 * IO Exceptions
 */
public class ThreescaleCmsNonApiException extends ThreescaleCmsException {
    public ThreescaleCmsNonApiException(String message) {
        super(message);
    }

    public ThreescaleCmsNonApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
