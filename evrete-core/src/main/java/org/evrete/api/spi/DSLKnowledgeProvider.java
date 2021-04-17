package org.evrete.api.spi;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RuntimeContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

public interface DSLKnowledgeProvider {

    String getName();

    /**
     * <p>
     * Given the sources' URLs, the DSL implementation must return a new Knowledge instance.
     * Depending on the DSL implementation, URLs may refer to plain text resources, Java classes/archives,
     * JDBC connection strings, files, etc.
     * </p>
     *
     * @param resources remote or local resources to apply
     * @param service   Knowledge service.
     */
    Knowledge create(KnowledgeService service, URL... resources) throws IOException;

    /**
     * @param streams remote or local resources to apply
     * @param service Knowledge service.
     * @see #create(KnowledgeService, URL...)
     */
    Knowledge create(KnowledgeService service, InputStream... streams) throws IOException;

    /**
     * @param streams remote or local resources to apply
     * @param service Knowledge service.
     * @throws IOException                   if resources can not be read
     * @throws UnsupportedOperationException if this method is not supported by the implementation
     * @see #create(KnowledgeService, URL...)
     */
    default Knowledge create(KnowledgeService service, Reader... streams) throws IOException {
        throw new UnsupportedOperationException("Method not supported by " + getClass().getName());
    }

    /**
     * <p>
     * Given the sources' URLs, the DSL implementation must compile and append
     * rules to the provided context.
     * </p>
     *
     * @param targetContext the runtime to append rules to
     * @param resources     remote or local resources to apply
     */
    @Deprecated
    default void apply(RuntimeContext<?> targetContext, URL... resources) throws IOException {
        throw new UnsupportedOperationException("Method not supported by " + getClass().getName());
    }

    /**
     * <p>
     * Given the input stream, the DSL implementation must compile and append
     * rules to the provided context.
     * </p>
     *
     * @param targetContext the runtime to append rules to
     * @param inputStreams  input streams to ruleset sources
     */
    @Deprecated
    default void apply(RuntimeContext<?> targetContext, InputStream... inputStreams) throws IOException {
        throw new UnsupportedOperationException("Method not supported by " + getClass().getName());
    }

    /**
     * @param targetContext the runtime to append rules to
     * @param readers        readers of ruleset sources
     * @see #apply(RuntimeContext, InputStream...)
     */
    @Deprecated
    default void apply(RuntimeContext<?> targetContext, Reader... readers) throws IOException {
        throw new UnsupportedOperationException("Method not supported by " + getClass().getName());
    }
}
