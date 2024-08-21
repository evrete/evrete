package org.evrete.dsl;

import org.evrete.api.RuntimeContext;

import java.util.*;

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
    <C extends RuntimeContext<C>> ResourceClasses createFromClasses(RuntimeContext<C> context, Collection<Class<?>> resources) {
        if(resources == null || resources.isEmpty()) {
            return null;
        } else {
            return new ResourceClasses(context.getClassLoader(), resources);
        }
    }

    @Override
    <C extends RuntimeContext<C>> ResourceClasses createFromStrings(RuntimeContext<C> context, Collection<CharSequence> resources) {
        ClassLoader classLoader = context.getClassLoader();
        try {
            Collection<Class<?>> classResources = new ArrayList<>(resources.size());
            for (CharSequence resource : resources) {
                Class<?> cl = Class.forName(resource.toString(), true, classLoader);
                classResources.add(cl);
            }
            return createFromClasses(context, classResources);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find class " + resources, e);
        }
    }

    @Override
    protected Set<Class<?>> sourceTypes() {
        return new HashSet<>(Arrays.asList(SUPPORTED_TYPES));
    }

    @Override
    public String getName() {
        return PROVIDER_JAVA_CLASS;
    }

}
