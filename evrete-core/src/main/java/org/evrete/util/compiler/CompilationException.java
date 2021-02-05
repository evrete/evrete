package org.evrete.util.compiler;

class CompilationException extends RuntimeException {

    private static final long serialVersionUID = -8017644675581374126L;

    CompilationException(String message) {
        super(message);
    }

    CompilationException(Throwable cause) {
        super(cause);
    }
}
