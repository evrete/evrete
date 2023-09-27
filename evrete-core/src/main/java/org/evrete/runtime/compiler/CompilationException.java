package org.evrete.runtime.compiler;

import org.evrete.api.JavaSourceCompiler;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CompilationException extends Exception {

    private static final long serialVersionUID = -8017644675581374126L;

    private final List<String> otherErrors;
    private final Map<JavaSourceCompiler.ClassSource, String> errorSources;

    CompilationException(List<String> otherErrors, Map<JavaSourceCompiler.ClassSource, String> errorSources) {
        this.otherErrors = otherErrors;
        this.errorSources = errorSources;
    }

    public List<String> getOtherErrors() {
        return otherErrors;
    }

    public Collection<JavaSourceCompiler.ClassSource> getErrorSources() {
        return errorSources.keySet();
    }

    public String getErrorMessage(JavaSourceCompiler.ClassSource source) {
        return errorSources.get(source);
    }
}
