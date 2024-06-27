package org.evrete.dsl;

import org.evrete.api.JavaSourceCompiler;
import org.evrete.api.RuntimeContext;
import org.evrete.util.JavaSourceUtils;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

class DSLMetaLiteralSource<C extends RuntimeContext<C>> extends DSLMeta<C> {
    private final String source;
    private RulesClass preparedData;

    DSLMetaLiteralSource(MethodHandles.Lookup globalPublicLookup, String source) {
        super(globalPublicLookup);
        this.source = source;
    }

    @Override
    JavaSourceCompiler.ClassSource sourceToCompile() {
        return JavaSourceUtils.parse(this.source);
    }

    @Override
    void applyCompiledSource(Class<?> compiledClass) {
        WrappedClass wrappedClass = new WrappedClass(compiledClass, globalPublicLookup);
        this.preparedData = new RulesClass(wrappedClass);
    }

    @Override
    RulesClass getPreparedData() {
        return Objects.requireNonNull(preparedData);
    }
}
