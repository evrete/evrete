package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RuntimeContext;
import org.evrete.api.TypeResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The DSLClassProvider class provides the implementation of the DSLKnowledgeProvider
 * interface for 'JAVA-CLASS' DSL knowledge.
 */
public class DSLClassProvider extends AbstractDSLProvider {
    private static final Class<?>[] SUPPORTED_TYPES = new Class<?>[] {
            TYPE_CLASS,
            TYPE_CHAR_SEQUENCE
    };

    /**
     * Default public constructor
     */
    public DSLClassProvider() {
    }

    @Override
    public Optional<Class<?>[]> sourceTypes() {
        return Optional.of(SUPPORTED_TYPES);
    }

    @Override
    <C extends RuntimeContext<C>> Stream<Class<?>> sourceClasses(RuntimeContext<C> context, Class<?>[] classes) {
        return Stream.of(classes);
    }

    @Override
    <C extends RuntimeContext<C>> Stream<Class<?>> sourceClasses(RuntimeContext<C> context, CharSequence[] strings) throws IOException {
        return sourceClasses(context, loadClasses(context.getClassLoader(), strings));
    }

    @Override
    public String getName() {
        return PROVIDER_JAVA_CLASS;
    }


    @Override
    public Knowledge create(KnowledgeService service, URL... resources) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Knowledge create(KnowledgeService service, InputStream... streams) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Knowledge create(KnowledgeService service, TypeResolver typeResolver, Reader... streams) throws IOException {
        return create(service, loadClasses(service.getClassLoader(), streams));
    }

    private Knowledge create(KnowledgeService service, Class<?>... classes) throws IOException {

        Knowledge knowledge = service.newKnowledge();
        return knowledge
                .builder()
                .importAllRules(this, classes)
                .build();
    }

    private static Class<?>[] loadClasses(ClassLoader classLoader, Reader... streams) throws IOException {
        Class<?>[] classes = new Class<?>[streams.length];

        for (int i = 0; i < streams.length; i++) {
            BufferedReader br = new BufferedReader(streams[i]);
            String className = br.readLine();
            try {
                classes[i] = classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new IOException("Unable to load class '" + className + "'", e);
            }
        }
        return classes;
    }

    private static Class<?>[] loadClasses(ClassLoader classLoader, CharSequence... streams) throws IOException {
        Class<?>[] classes = new Class<?>[streams.length];
        for (int i = 0; i < streams.length; i++) {
            String className = streams[i].toString();
            try {
                classes[i] = classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new IOException("Unable to load class '" + className + "'", e);
            }
        }
        return classes;
    }
}
