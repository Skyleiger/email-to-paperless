package de.dwienzek.emailtopaperless.exception;

public class EmailFetchException extends Exception {

    public EmailFetchException() {
    }

    public EmailFetchException(String message) {
        super(message);
    }

    public EmailFetchException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailFetchException(Throwable cause) {
        super(cause);
    }

    public EmailFetchException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}