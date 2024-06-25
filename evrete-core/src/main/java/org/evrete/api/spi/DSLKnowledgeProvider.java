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
 * An interface for creating and integrating Domain-Specific Language (DSL) implementations with the rule engine.
 * Implementations can utilize any DSL format, ranging from plain text to database storage. When an implementation
 * is available on the classpath, developers can use it through the
 * {@link RuleSetBuilder#importRules(String, Object)} method.
 *
 * <p>
 * If an implementation requires or relies on specific configuration options, the authors of the implementation
 * should read them via the {@link RuleSetBuilder#get(String)} method. The configuration options should be documented
 * so that developers can set those options via the corresponding {@link RuleSetBuilder#set(String, Object)} method
 * prior to importing the rules.
 * </p>
 */
public interface DSLKnowledgeProvider {

    /**
     * Returns the name of the DSL knowledge provider.
     *
     * @return the name of the provider
     */
    String getName();


    /**
     * Appends the provided rule source to the specified {@link RuleSetBuilder} instance.
     *
     * <p>
     * If the implementation requires or relies on specific configuration entries,
     * they should be set via the {@link RuleSetBuilder#set(String, Object)} method.
     * </p>
     *
     * <p>
     * This method is the preferred approach for integrating external rule sources. Unlike the deprecated
     * {@link #create(KnowledgeService, TypeResolver, URL...)} method and its variants, this method enables
     * appending sources directly to both {@link Knowledge} and
     * {@link org.evrete.api.RuleSession} rule set instances.
     * </p>
     *
     * @param <C>    The type of the context for the builder.
     * @param target The {@link RuleSetBuilder} instance to which the rule source(s) will be appended.
     * @param source The rule source to be appended to the target builder. The source can be in various
     *               formats, depending on the implementation (e.g., a String representing rule definitions,
     *               a File with rule definitions, a URL, etc.).
     * @throws IOException              If an I/O error occurs while processing the source.
     * @throws IllegalArgumentException If the source format is unrecognized or cannot be processed.
     */
    default <C extends RuntimeContext<C>> void appendTo(@NonNull RuleSetBuilder<C> target, Object source) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Creates a new {@link Knowledge} from the provided resources. This method has been deprecated since version 4.0.0
     * in favor of using the new {@link #appendTo(RuleSetBuilder, Object)} method for applying external resources.
     * To ensure backward compatibility, this method (and other similar pre-4.0.0 methods) now attempts to
     * automatically adopt the new approach. However, depending on the implementation,
     * compatibility might not be fully guaranteed.
     *
     * @param service   the Knowledge service.
     * @param resources the remote or local resources to apply.
     * @return a new Knowledge instance.
     * @throws IOException if an error occurs while reading the data sources.
     * @deprecated Use the {@link #appendTo(RuleSetBuilder, Object)} method instead.
     */
    @Deprecated
    default Knowledge create(KnowledgeService service, URL... resources) throws IOException {
        Knowledge knowledge = service.newKnowledge();
        RuleSetBuilder<Knowledge> ruleSetBuilder = knowledge.builder();
        for (URL resource : resources) {
            this.appendTo(ruleSetBuilder, resource);
        }
        return ruleSetBuilder.build();
    }

    /**
     * Creates a new {@link Knowledge} from the provided resources. This method has been deprecated since version 4.0.0
     * in favor of using the new {@link #appendTo(RuleSetBuilder, Object)} method for applying external resources.
     * To ensure backward compatibility, this method (and other similar pre-4.0.0 methods) now attempts to
     * automatically adopt the new approach. However, depending on the implementation,
     * compatibility might not be fully guaranteed.
     *
     * @param resources    remote or local resources to apply
     * @param typeResolver TypeResolver to use.
     * @param service      Knowledge service.
     * @return new Knowledge instance
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated Use the {@link #appendTo(RuleSetBuilder, Object)} method instead.
     */
    @Deprecated
    @SuppressWarnings("unused")
    default Knowledge create(KnowledgeService service, TypeResolver typeResolver, URL... resources) throws IOException {
        return create(service, resources);
    }

    /**
     * Creates a new {@link Knowledge} from the provided resources. This method has been deprecated since version 4.0.0
     * in favor of using the new {@link #appendTo(RuleSetBuilder, Object)} method for applying external resources.
     * To ensure backward compatibility, this method (and other similar pre-4.0.0 methods) now attempts to
     * automatically adopt the new approach. However, depending on the implementation,
     * compatibility might not be fully guaranteed.
     *
     * @param streams remote or local resources to apply
     * @param service Knowledge service.
     * @return new Knowledge instance
     * @throws IOException if an error occurs when reading the data sources.
     * @see #create(KnowledgeService, URL...)
     * @deprecated Use the {@link #appendTo(RuleSetBuilder, Object)} method instead.
     */
    @Deprecated
    default Knowledge create(KnowledgeService service, InputStream... streams) throws IOException {
        Knowledge knowledge = service.newKnowledge();
        RuleSetBuilder<Knowledge> ruleSetBuilder = knowledge.builder();
        for (InputStream stream : streams) {
            this.appendTo(ruleSetBuilder, stream);
        }
        return ruleSetBuilder.build();
    }

    /**
     * Creates a new {@link Knowledge} from the provided resources. This method has been deprecated since version 4.0.0
     * in favor of using the new {@link #appendTo(RuleSetBuilder, Object)} method for applying external resources.
     * To ensure backward compatibility, this method (and other similar pre-4.0.0 methods) now attempts to
     * automatically adopt the new approach. However, depending on the implementation,
     * compatibility might not be fully guaranteed.
     *
     * @param files    file resources
     * @param service  Knowledge service.
     * @param resolver Type resolver
     * @return new Knowledge instance
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated Use the {@link #appendTo(RuleSetBuilder, Object)} method instead.
     */
    @Deprecated
    @SuppressWarnings("unused")
    default Knowledge create(KnowledgeService service, TypeResolver resolver, File... files) throws IOException {
        return create(service, files);
    }

    /**
     * Creates a new {@link Knowledge} from the provided resources. This method has been deprecated since version 4.0.0
     * in favor of using the new {@link #appendTo(RuleSetBuilder, Object)} method for applying external resources.
     * To ensure backward compatibility, this method (and other similar pre-4.0.0 methods) now attempts to
     * automatically adopt the new approach. However, depending on the implementation,
     * compatibility might not be fully guaranteed.
     *
     * @param files   file resources
     * @param service Knowledge service.
     * @return new Knowledge instance
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated Use the {@link #appendTo(RuleSetBuilder, Object)} method instead.
     */
    @Deprecated
    default Knowledge create(KnowledgeService service, File... files) throws IOException {
        URL[] urls = new URL[files.length];
        for (int i = 0; i < files.length; i++) {
            urls[i] = files[i].toURI().toURL();
        }

        return create(service, urls);
    }

    /**
     * Creates a new {@link Knowledge} from the provided resources. This method has been deprecated since version 4.0.0
     * in favor of using the new {@link #appendTo(RuleSetBuilder, Object)} method for applying external resources.
     * To ensure backward compatibility, this method (and other similar pre-4.0.0 methods) now attempts to
     * automatically adopt the new approach. However, depending on the implementation,
     * compatibility might not be fully guaranteed.
     *
     * @param streams      remote or local resources to apply
     * @param service      Knowledge service.
     * @param typeResolver TypeResolver to use.
     * @return new Knowledge instance
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated Use the {@link #appendTo(RuleSetBuilder, Object)} method instead.
     */
    @Deprecated
    @SuppressWarnings("unused")
    default Knowledge create(KnowledgeService service, TypeResolver typeResolver, InputStream... streams) throws IOException {
        return create(service, streams);
    }

    /**
     * Creates a new {@link Knowledge} from the provided resources. This method has been deprecated since version 4.0.0
     * in favor of using the new {@link #appendTo(RuleSetBuilder, Object)} method for applying external resources.
     * To ensure backward compatibility, this method (and other similar pre-4.0.0 methods) now attempts to
     * automatically adopt the new approach. However, depending on the implementation,
     * compatibility might not be fully guaranteed.
     *
     * @param streams remote or local resources to apply
     * @param service Knowledge service.
     * @return new Knowledge instance
     * @throws IOException                   if resources can not be read
     * @throws UnsupportedOperationException if this method is not supported by the implementation
     * @see #create(KnowledgeService, URL...)
     * @deprecated Use the {@link #appendTo(RuleSetBuilder, Object)} method instead.
     */
    @Deprecated
    default Knowledge create(KnowledgeService service, Reader... streams) throws IOException {
        Knowledge knowledge = service.newKnowledge();
        RuleSetBuilder<Knowledge> ruleSetBuilder = knowledge.builder();
        for (Reader stream : streams) {
            this.appendTo(ruleSetBuilder, stream);
        }
        return ruleSetBuilder.build();
    }

    /**
     * Creates a new {@link Knowledge} from the provided resources. This method has been deprecated since version 4.0.0
     * in favor of using the new {@link #appendTo(RuleSetBuilder, Object)} method for applying external resources.
     * To ensure backward compatibility, this method (and other similar pre-4.0.0 methods) now attempts to
     * automatically adopt the new approach. However, depending on the implementation,
     * compatibility might not be fully guaranteed.
     *
     * @param streams      The remote or local resources to apply.
     * @param service      The Knowledge service.
     * @param typeResolver The TypeResolver to use.
     * @return A new instance of Knowledge.
     * @throws IOException                   If the resources cannot be read.
     * @throws UnsupportedOperationException If this method is not supported by the implementation.
     * @deprecated Use the {@link #appendTo(RuleSetBuilder, Object)} method instead.
     */
    @Deprecated
    @SuppressWarnings("unused")
    default Knowledge create(KnowledgeService service, TypeResolver typeResolver, Reader... streams) throws IOException {
        return create(service, streams);
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
