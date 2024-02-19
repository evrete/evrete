package org.evrete.spi.minimal;

import org.evrete.api.JavaSourceCompiler;
import org.evrete.api.LiteralExpression;
import org.evrete.api.Rule;
import org.evrete.runtime.compiler.CompilationException;

import java.util.*;

public class RuleCompilationException extends CompilationException {
    private final Collection<RuleCompilationException.Failure> failures;

    public RuleCompilationException(Collection<RuleCompilationException.Failure> failures, Collection<String> otherErrors) {
        super(new ArrayList<>(otherErrors), asMap(failures));
        this.failures = failures;
    }

    private static Map<JavaSourceCompiler.ClassSource, String> asMap(Collection<RuleCompilationException.Failure> failures) {
        Map<JavaSourceCompiler.ClassSource, String> map = new HashMap<>(failures.size());
        //TODO !!!! fix the compilation exception class
//        for(Failure failure : failures) {
//            map.put(failure.source, failure.error);
//        }
        return map;
    }

    public static class Failure {
        final Rule rule;
        final String source;
        final List<String> errors;

        public Failure(Rule rule, String source, List<String> errors) {
            this.rule = rule;
            this.source = source;
            this.errors = errors;
        }
        public Failure(Rule rule, String source, String error) {
            this(rule, source, Collections.singletonList(error));
        }
    }
}
