package org.evrete.util;

import org.evrete.api.JavaSourceCompiler;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CompilationException extends Exception {

    private static final long serialVersionUID = -8017644675581374126L;

    private final List<String> otherErrors;
    private final Map<JavaSourceCompiler.ClassSource, List<String>> errorSources;

    public CompilationException(List<String> otherErrors, Map<JavaSourceCompiler.ClassSource, List<String>> errorSources) {
        super("Source compilation error. Failed sources: " + errorSources.size() + ", other errors: " + otherErrors.size() + ", see application logs for details.");
        this.otherErrors = otherErrors;
        this.errorSources = errorSources;
    }

    public void log(Logger logger, Level level) {
        for(JavaSourceCompiler.ClassSource s : getErrorSources()) {
            List<String> sourceErrors = getErrorMessage(s);
            for (String error : sourceErrors) {
                logger.log(level, error);
            }

            logger.log(level, "\n--- Java Source Start ---\n" + s.getSource() + "\n---  Java Source End  ---\n");
        }
        if (!otherErrors.isEmpty()) {
            logger.log(level, "Other errors:\n" + otherErrors);
        }
    }

    public Collection<JavaSourceCompiler.ClassSource> getErrorSources() {
        return errorSources.keySet();
    }

    public List<String> getErrorMessage(JavaSourceCompiler.ClassSource source) {
        return errorSources.get(source);
    }
}
