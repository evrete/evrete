package org.evrete.runtime.compiler;

import org.evrete.api.JavaSourceCompiler;
import org.evrete.api.LiteralExpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LHSCompilationException extends CompilationException {
    private final Collection<LHSCompilationException.Failure> failures;

    public LHSCompilationException(Collection<LHSCompilationException.Failure> failures, Collection<String> otherErrors) {
        super(new ArrayList<>(otherErrors), asMap(failures));
        this.failures = failures;
    }

    private static Map<JavaSourceCompiler.ClassSource, String> asMap(Collection<LHSCompilationException.Failure> failures) {
        Map<JavaSourceCompiler.ClassSource, String> map = new HashMap<>(failures.size());
        for(Failure failure : failures) {
            map.put(failure.source, failure.error);
        }
        return map;
    }

    public static class Failure {
        final LiteralExpression expression;

        final JavaSourceCompiler.ClassSource source;
        final String error;

        public Failure(JavaSourceCompiler.ClassSource source, LiteralExpression expression, String error) {
            this.source = source;
            this.expression = expression;
            this.error = error;
        }
    }
}
