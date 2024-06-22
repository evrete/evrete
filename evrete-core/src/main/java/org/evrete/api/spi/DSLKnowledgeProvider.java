package org.evrete.api.spi;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RuntimeContext;
import org.evrete.api.TypeResolver;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.builders.RuleSetBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;

/**
 * DSLKnowledgeProvider is an interface that provides methods to build rules from various types of resources.
 */
public interface DSLKnowledgeProvider {

    /**
     * Returns the name of the DSL knowledge provider.
     *
     * @return the name of the provider
     */
    String getName();

    /**
     * Returns the types that can be used as rule sources.
     *
     * @return An {@link Optional} containing an array of {@link Class} objects. These objects represent the valid component types
     * for rule sources, or an empty {@link Optional} if there are no specific component types applicable.
     */
    default Optional<Class<?>[]> sourceTypes() {
        return Optional.empty();
    }

    /**
     * Appends the provided rule source(s) to the specified {@link RuleSetBuilder} instance.
     * <p>
     * The implementation can report the valid types of arguments it accepts via
     * the {@link #sourceTypes()} method.
     * </p>
     * <p>
     * This method represents the preferred approach for integrating external rule sources. In contrast
     * to the {@link #create(KnowledgeService, TypeResolver, URL...)} method and its variants,
     * this method enables appending sources directly to both {@link Knowledge} and
     * {@link org.evrete.api.RuleSession} rule set instances.
     * </p>
     *
     * @param <C>        The type of the context of the builder.
     * @param target     The {@link RuleSetBuilder} instance to which the rule source(s) will be appended.
     * @param nameFilter A filter parameter that allows for the selection of specific ruleset name(s)
     *                   since each source may contain multiple rulesets.
     * @param sources    The rule source to be appended to the target builder. The source can be in various
     *                   formats, depending on the implementation (e.g., a String representing rule definitions,
     *                   a File with rule definitions, a URL, etc.).
     * @throws IOException              if an I/O error occurs while processing the source.
     * @throws IllegalArgumentException if the source format is unrecognized or cannot be processed.
     */
    default <C extends RuntimeContext<C>> void appendTo(@NonNull RuleSetBuilder<C> target, @NonNull Predicate<String> nameFilter, Object[] sources) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Creates a new Knowledge instance based on the given sources' URLs. Depending on the DSL implementation,
     * the URLs may refer to plain text resources, Java classes/archives, JDBC connection strings, files, etc.
     *
     * <p>
     * <strong>Warning:</strong> This method has not been marked as deprecated yet; however, developers are encouraged
     * to transition to the new {@link #appendTo(RuleSetBuilder, Predicate, Object[])} method for applying external rulesets.
     * </p>
     *
     * @param service   the Knowledge service.
     * @param resources the remote or local resources to apply.
     * @return a new Knowledge instance.
     * @throws IOException if an error occurs while reading the data sources.
     */
    Knowledge create(KnowledgeService service, URL... resources) throws IOException;

    /**
     * <p>
     * Given the sources' URLs, the DSL implementation must return a new Knowledge instance.
     * Depending on the DSL implementation, URLs may refer to plain text resources, Java classes/archives,
     * JDBC connection strings, files, etc.
     * </p>
     * <p>
     * <strong>Warning:</strong> This method has not been marked as deprecated yet; however, developers are encouraged
     * to transition to the new {@link #appendTo(RuleSetBuilder, Predicate, Object[])} method for applying external rulesets.
     * </p>
     *
     * @param resources    remote or local resources to apply
     * @param typeResolver TypeResolver to use.
     * @param service      Knowledge service.
     * @return new Knowledge instance
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated use the {@link #create(KnowledgeService, URL...)} method instead
     */
    @Deprecated
    @SuppressWarnings("unused")
    default Knowledge create(KnowledgeService service, TypeResolver typeResolver, URL... resources) throws IOException {
        return create(service, resources);
    }

    /**
     * <p>
     * <strong>Warning:</strong> This method has not been marked as deprecated yet; however, developers are encouraged
     * to transition to the new {@link #appendTo(RuleSetBuilder, Predicate, Object[])} method for applying external rulesets.
     * </p>
     *
     * @param streams remote or local resources to apply
     * @param service Knowledge service.
     * @return new Knowledge instance
     * @throws IOException if an error occurs when reading the data sources.
     * @see #create(KnowledgeService, URL...)
     */
    Knowledge create(KnowledgeService service, InputStream... streams) throws IOException;

    /**
     * <p>
     * <strong>Warning:</strong> This method has not been marked as deprecated yet; however, developers are encouraged
     * to transition to the new {@link #appendTo(RuleSetBuilder, Predicate, Object[])} method for applying external rulesets.
     * </p>
     *
     * @param files    file resources
     * @param service  Knowledge service.
     * @param resolver Type resolver
     * @return new Knowledge instance
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated use the {@link #create(KnowledgeService, File...)} method instead
     */
    @Deprecated
    @SuppressWarnings("unused")
    default Knowledge create(KnowledgeService service, TypeResolver resolver, File... files) throws IOException {
        return create(service, files);
    }

    /**
     * <p>
     * <strong>Warning:</strong> This method has not been marked as deprecated yet; however, developers are encouraged
     * to transition to the new {@link #appendTo(RuleSetBuilder, Predicate, Object[])} method for applying external rulesets.
     * </p>
     *
     * @param files   file resources
     * @param service Knowledge service.
     * @return new Knowledge instance
     * @throws IOException if an error occurs when reading the data sources.
     * @see #create(KnowledgeService, URL...)
     */
    default Knowledge create(KnowledgeService service, File... files) throws IOException {
        URL[] urls = new URL[files.length];
        for (int i = 0; i < files.length; i++) {
            urls[i] = files[i].toURI().toURL();
        }

        return create(service, urls);
    }

    /**
     * <p>
     * <strong>Warning:</strong> This method has not been marked as deprecated yet; however, developers are encouraged
     * to transition to the new {@link #appendTo(RuleSetBuilder, Predicate, Object[])} method for applying external rulesets.
     * </p>
     *
     * @param streams      remote or local resources to apply
     * @param service      Knowledge service.
     * @param typeResolver TypeResolver to use.
     * @return new Knowledge instance
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated use the {@link #create(KnowledgeService, InputStream...)} method instead
     */
    @Deprecated
    @SuppressWarnings("unused")
    default Knowledge create(KnowledgeService service, TypeResolver typeResolver, InputStream... streams) throws IOException {
        return create(service, streams);
    }

    /**
     * <p>
     * <strong>Warning:</strong> This method has not been marked as deprecated yet; however, developers are encouraged
     * to transition to the new {@link #appendTo(RuleSetBuilder, Predicate, Object[])} method for applying external rulesets.
     * </p>
     *
     * @param streams remote or local resources to apply
     * @param service Knowledge service.
     * @return new Knowledge instance
     * @throws IOException                   if resources can not be read
     * @throws UnsupportedOperationException if this method is not supported by the implementation
     * @see #create(KnowledgeService, URL...)
     */
    default Knowledge create(KnowledgeService service, Reader... streams) throws IOException {
        throw new UnsupportedOperationException("Method not supported by " + getClass().getName());
    }

    /**
     * <p>
     * <strong>Warning:</strong> This method has not been marked as deprecated yet; however, developers are encouraged
     * to transition to the new {@link #appendTo(RuleSetBuilder, Predicate, Object[])} method for applying external rulesets.
     * </p>
     *
     * @param streams      remote or local resources to apply
     * @param service      Knowledge service.
     * @param typeResolver TypeResolver to use.
     * @return new Knowledge instance
     * @throws IOException                   if resources can not be read
     * @throws UnsupportedOperationException if this method is not supported by the implementation
     * @see #create(KnowledgeService, URL...)
     */
    default Knowledge create(KnowledgeService service, TypeResolver typeResolver, Reader... streams) throws IOException {
        throw new UnsupportedOperationException("Method not supported by " + getClass().getName());
    }

    /**
     * Loads a specific DSL (Domain-Specific Language) knowledge provider based on the provided identifier.
     *
     * @param dsl The identifier for the DSL provider to load.
     * @return The DSLKnowledgeProvider implementation matching the given identifier.
     * @throws NullPointerException  if the dsl argument is null.
     * @throws IllegalStateException if no or multiple providers were found for the specified DSL identifier.
     */
    static DSLKnowledgeProvider load(@NonNull String dsl) {
        Objects.requireNonNull(dsl, "DSL identifier cannot be null");
        ServiceLoader<DSLKnowledgeProvider> loader = ServiceLoader.load(DSLKnowledgeProvider.class);

        List<DSLKnowledgeProvider> found = new LinkedList<>();
        StringJoiner knownProviders = new StringJoiner(", ", "[", "]");
        for (DSLKnowledgeProvider provider : loader) {
            String name = provider.getName();
            if (dsl.equals(name)) {
                found.add(provider);
            }
            knownProviders.add("'" + name + "' = " + provider.getClass().getName());
        }

        if (found.isEmpty()) {
            throw new IllegalStateException("DSL provider '" + dsl + "' is not found. Make sure the corresponding implementation is available on the classpath. Available providers: " + knownProviders);
        }

        if (found.size() > 1) {
            throw new IllegalStateException("Multiple DSL providers found implementing the '" + dsl + "' language. Known providers: " + knownProviders);
        } else {
            return found.iterator().next();
        }
    }

    /**
     * Attempts to load and instantiate a {@link DSLKnowledgeProvider} implementation.
     *
     * @param dsl The {@code Class} object corresponding to the DSL implementation class.
     *            This class must implement the {@code DSLKnowledgeProvider} interface and
     *            have a public no-argument constructor. Must not be {@code null}.
     * @return An instance of the specified {@code DSLKnowledgeProvider} implementation.
     * @throws IllegalStateException If the instantiation of the DSL class instance fails
     *                               for any reason, including if the input parameter is
     *                               {@code null}.
     */
    static DSLKnowledgeProvider load(Class<? extends DSLKnowledgeProvider> dsl) {
        Objects.requireNonNull(dsl, "The DSL class must not be null.");
        try {
            return dsl.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to instantiate DSL class instance: " + dsl.getName(), e);
        }
    }
}
