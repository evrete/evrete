package org.evrete.util.compiler;

public class CompilationException extends Exception {

    private static final long serialVersionUID = -8017644675581374126L;
    private final String source;

    CompilationException(String message, String source) {
        super(message);
        this.source = source;
    }

    public CompilationException(Throwable cause, String source) {
        super(cause);
        this.source = source;
    }

    public String getSource() {
        return source;
    }
}
