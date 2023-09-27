package org.evrete.runtime.compiler;

import org.evrete.api.JavaSourceCompiler;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CompilationException extends Exception {

    private static final long serialVersionUID = -8017644675581374126L;

    private final List<String> otherErrors;
    private final Map<JavaSourceCompiler.ClassSource, String> errorSources;

    CompilationException(List<String> otherErrors, Map<JavaSourceCompiler.ClassSource, String> errorSources) {
        super("Source compilation error. Failed sources: " + errorSources.size() + ", other errors: " + otherErrors.size() + ", see application logs for details.");
        this.otherErrors = otherErrors;
        this.errorSources = errorSources;
    }

    public void log(Logger logger, Level level) {
        for(JavaSourceCompiler.ClassSource s : getErrorSources()) {
            String error = getErrorMessage(s);
            logger.log(level, error + "\nin source:\n" + s.getSource());
        }
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
