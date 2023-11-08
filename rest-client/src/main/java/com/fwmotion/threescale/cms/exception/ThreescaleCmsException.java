package com.fwmotion.threescale.cms.exception;

/**
 * Common base exception type for all 3scale CMS-related exceptions
 */
public class ThreescaleCmsException extends RuntimeException {

    protected ThreescaleCmsException(String message) {
        super(message);
    }

    protected ThreescaleCmsException(String message, Throwable cause) {
        super(message, cause);
    }

}
