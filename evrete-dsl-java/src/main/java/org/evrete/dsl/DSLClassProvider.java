package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.TypeResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.net.URL;

/**
 * The DSLClassProvider class provides the implementation of the DSLKnowledgeProvider
 * interface for 'JAVA-CLASS' DSL knowledge.
 */
public class DSLClassProvider extends AbstractDSLProvider {

    /**
     * Default public constructor
     */
    public DSLClassProvider() {
    }

    private static Class<?>[] loadClasses(KnowledgeService service, Reader... streams) throws IOException {
        Class<?>[] classes = new Class<?>[streams.length];
        for (int i = 0; i < streams.length; i++) {

            BufferedReader br = new BufferedReader(streams[i]);
            String className = br.readLine();
            try {
                classes[i] = service.getClassLoader().loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Unable to load class '" + className + "'", e);
            }
        }
        return classes;
    }

    @Override
    public String getName() {
        return PROVIDER_JAVA_CLASS;
    }

    @Override
    public Knowledge create(KnowledgeService service, TypeResolver typeResolver, InputStream... streams) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Knowledge create(KnowledgeService service, URL... resources) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Knowledge create(KnowledgeService service, TypeResolver typeResolver, URL... resources) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Knowledge create(KnowledgeService service, InputStream... streams) throws IOException {
        return super.create(service, streams);
    }

    @Override
    public Knowledge create(KnowledgeService service, Reader... streams) throws IOException {
        return super.create(service, streams);
    }

    @Override
    public Knowledge create(KnowledgeService service, TypeResolver typeResolver, Reader... streams) throws IOException {

        Class<?>[] classes = loadClasses(service, streams);

        Knowledge current = service.newKnowledge(typeResolver);
        MethodHandles.Lookup lookup = defaultLookup();
        for (Class<?> cl : classes) {
            current = processRuleSet(current, lookup, cl);
        }
        return current;
    }
}
