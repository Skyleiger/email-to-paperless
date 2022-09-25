package de.dwienzek.emailtopaperless.exception;

public class UnknownEmailContentException extends Exception {

    public UnknownEmailContentException(String message, Object... parameters) {
        super(String.format(message, parameters));
    }

}
