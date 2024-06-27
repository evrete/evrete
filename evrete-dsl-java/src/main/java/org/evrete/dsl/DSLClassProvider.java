package org.evrete.dsl;

import org.evrete.api.RuntimeContext;
import org.evrete.api.builders.RuleSetBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * The DSLClassProvider class provides the implementation of the DSLKnowledgeProvider
 * interface for 'JAVA-CLASS' DSL knowledge.
 */
public class DSLClassProvider extends AbstractDSLProvider {
    private static final Class<?>[] SUPPORTED_TYPES = new Class<?>[] {
            TYPE_CLASS,
            TYPE_CHAR_SEQUENCE
    };

    @Override
    <C extends RuntimeContext<C>> Collection<DSLMeta<C>> createClassMeta(RuleSetBuilder<C> target, Class<?> clazz) {
        return List.of(new DSLMetaClassSource<>(publicLookup, clazz));
    }

    @Override
    <C extends RuntimeContext<C>> Collection<DSLMeta<C>> createClassMeta(RuleSetBuilder<C> target, CharSequence literal) {
        ClassLoader classLoader = target.getContext().getClassLoader();
        try {
            Class<?> cl = Class.forName(literal.toString(), true, classLoader);
            return createClassMeta(target, cl);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find class " + literal, e);
        }
    }

    @Override
    protected Set<Class<?>> sourceTypes() {
        return Set.of(SUPPORTED_TYPES);
    }

    @Override
    public String getName() {
        return PROVIDER_JAVA_CLASS;
    }

}
