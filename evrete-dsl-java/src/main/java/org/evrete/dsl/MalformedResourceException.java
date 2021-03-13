package org.evrete.dsl;

class MalformedResourceException extends RuntimeException {

    private static final long serialVersionUID = 1480349194969454505L;

    MalformedResourceException(String message) {
        super(message);
    }

    MalformedResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
