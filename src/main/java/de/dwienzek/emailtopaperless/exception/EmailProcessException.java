package de.dwienzek.emailtopaperless.exception;

public class EmailProcessException extends Exception {

    public EmailProcessException() {
    }

    public EmailProcessException(String message) {
        super(message);
    }

    public EmailProcessException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailProcessException(Throwable cause) {
        super(cause);
    }

    public EmailProcessException(String message, Throwable cause, boolean enableSuppression,
                                 boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}