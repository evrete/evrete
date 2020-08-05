package org.evrete.spi.minimal;

class JcCompilationException extends RuntimeException {

    private static final long serialVersionUID = -8017644675581374126L;

    public JcCompilationException(String message) {
        super(message);
    }

    public JcCompilationException(Throwable cause) {
        super(cause);
    }
}
