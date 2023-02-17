package org.evrete.api.spi;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.TypeResolver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

public interface DSLKnowledgeProvider {

    String getName();

    default Knowledge create(KnowledgeService service, URL... resources) throws IOException {
        return create(service, service.newTypeResolver(), resources);
    }

    /**
     * <p>
     * Given the sources' URLs, the DSL implementation must return a new Knowledge instance.
     * Depending on the DSL implementation, URLs may refer to plain text resources, Java classes/archives,
     * JDBC connection strings, files, etc.
     * </p>
     *
     * @param resources    remote or local resources to apply
     * @param typeResolver TypeResolver to use.
     * @param service      Knowledge service.
     */
    @SuppressWarnings("resource")
    default Knowledge create(KnowledgeService service, TypeResolver typeResolver, URL... resources) throws IOException {
        if (resources == null || resources.length == 0) throw new IOException("Empty resources");
        InputStream[] streams = new InputStream[resources.length];
        try {
            for (int i = 0; i < resources.length; i++) {
                streams[i] = resources[i].openStream();
            }
            Knowledge knowledge = create(service, typeResolver, streams);

            for (InputStream stream : streams) {
                stream.close();
            }
            return knowledge;
        } finally {
            for (InputStream stream : streams) {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * @param streams remote or local resources to apply
     * @param service Knowledge service.
     * @see #create(KnowledgeService, URL...)
     */
    default Knowledge create(KnowledgeService service, InputStream... streams) throws IOException {
        return create(service, service.newTypeResolver(), streams);
    }

    /**
     * @param files    file resources
     * @param service  Knowledge service.
     * @param resolver Type resolver
     * @see #create(KnowledgeService, URL...)
     */
    default Knowledge create(KnowledgeService service, TypeResolver resolver, File... files) throws IOException {
        URL[] urls = new URL[files.length];
        for (int i = 0; i < files.length; i++) {
            urls[i] = files[i].toURI().toURL();
        }

        return create(service, resolver, urls);
    }

    /**
     * @param files   file resources
     * @param service Knowledge service.
     * @see #create(KnowledgeService, URL...)
     */
    default Knowledge create(KnowledgeService service, File... files) throws IOException {
        return create(service, service.newTypeResolver(), files);
    }

    /**
     * @param streams      remote or local resources to apply
     * @param service      Knowledge service.
     * @param typeResolver TypeResolver to use.
     * @see #create(KnowledgeService, TypeResolver, URL...)
     */
    Knowledge create(KnowledgeService service, TypeResolver typeResolver, InputStream... streams) throws IOException;

    /**
     * @param streams remote or local resources to apply
     * @param service Knowledge service.
     * @throws IOException                   if resources can not be read
     * @throws UnsupportedOperationException if this method is not supported by the implementation
     * @see #create(KnowledgeService, URL...)
     */
    default Knowledge create(KnowledgeService service, Reader... streams) throws IOException {
        return create(service, service.newTypeResolver(), streams);
    }

    /**
     * @param streams      remote or local resources to apply
     * @param service      Knowledge service.
     * @param typeResolver TypeResolver to use.
     * @throws IOException                   if resources can not be read
     * @throws UnsupportedOperationException if this method is not supported by the implementation
     * @see #create(KnowledgeService, URL...)
     */
    default Knowledge create(KnowledgeService service, TypeResolver typeResolver, Reader... streams) throws IOException {
        throw new UnsupportedOperationException("Method not supported by " + getClass().getName());
    }
}
