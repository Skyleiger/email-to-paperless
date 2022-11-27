package de.dwienzek.emailtopaperless.exception;

public class EmailStoreException extends Exception {

    public EmailStoreException() {
    }

    public EmailStoreException(String message) {
        super(message);
    }

    public EmailStoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailStoreException(Throwable cause) {
        super(cause);
    }

    public EmailStoreException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}