package org.evrete.dsl;

import org.evrete.api.JavaSourceCompiler;
import org.evrete.api.RuntimeContext;

import java.lang.invoke.MethodHandles;

class DSLMetaClassSource<C extends RuntimeContext<C>> extends DSLMeta<C> {
    private final RulesClass preparedData;

    DSLMetaClassSource(MethodHandles.Lookup globalPublicLookup, Class<?> classWithRules) {
        super(globalPublicLookup);
        this.preparedData = new RulesClass(new WrappedClass(classWithRules, globalPublicLookup));
    }

    @Override
    JavaSourceCompiler.ClassSource sourceToCompile() {
        return null;
    }

    @Override
    void applyCompiledSource(Class<?> compiledClass) {
        throw new IllegalStateException("Internal error, this method should not be called");
    }

    @Override
    RulesClass getPreparedData() {
        return preparedData;
    }
}
