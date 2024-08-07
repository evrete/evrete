package org.evrete.api;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.builders.RuleSetBuilder;
import org.evrete.api.events.EventBus;
import org.evrete.api.spi.DSLKnowledgeProvider;

import java.io.IOException;
import java.util.Comparator;
import java.util.function.Consumer;

/**
 * The RuntimeContext interface represents the context in which the rules are executed.
 * Both stateful and stateless sessions, as well as {@link Knowledge} instances, extend this interface.
 *
 * @param <C> the type of the implementing class or interface
 */
public interface RuntimeContext<C extends RuntimeContext<C>> extends FluentImports<C>, FluentEnvironment<C>, EventBus {

    /**
     * Retrieves the comparator used for ordering rules.
     *
     * @return the comparator for Rule objects.
     */
    Comparator<Rule> getRuleComparator();

    /**
     * Sets the comparator used for ordering rules.
     *
     * @param comparator the comparator for Rule objects.
     */
    void setRuleComparator(Comparator<Rule> comparator);


    /**
     * Return the condition manager associated with this context.
     * @return the evaluator context
     */
    EvaluatorsContext getEvaluatorsContext();

    /**
     * <p>
     * Returns an instance of {@link RuleSetBuilder} for building and appending rules to the current context.
     * </p>
     * <p>
     * Builder <strong>MUST</strong> be terminated with the {@link RuleSetBuilder#build()} call for changes to take effect.
     * </p>
     * <p> Usage BEFORE 3.1.00:</p>
     * <pre>
     * <code>
     * service
     *      .newKnowledge()
     *      .newRule()
     *      .forEach("$a", A.class)
     *      .where("$a.active")
     *      .execute();
     * </code>
     * </pre>
     * Usage AFTER 3.1.00:
     * <pre>
     * <code>
     * service
     *      .newKnowledge()
     *      .builder()       // !! important
     *      .newRule()
     *      .forEach("$a", A.class)
     *      .where("$a.active")
     *      .execute()
     *      .build();        // !! important
     * </code>
     * </pre>
     * @param classLoader the classloader to use.
     * @return new instance of RuleSetBuilder.
     * @since 3.1.00
     */
    RuleSetBuilder<C> builder(ClassLoader classLoader);

    /**
     * <p>
     * Returns an instance of {@link RuleSetBuilder} for building and appending rules to the current context.
     * </p>
     * <p>
     * Builder <strong>MUST</strong> be terminated with the {@link RuleSetBuilder#build()} call for changes to take effect.
     * </p>
     * <p> Usage BEFORE 3.1.00:</p>
     * <pre>
     * <code>
     * service
     *      .newKnowledge()
     *      .newRule()
     *      .forEach("$a", A.class)
     *      .where("$a.active")
     *      .execute();
     * </code>
     * </pre>
     * Usage AFTER 3.1.00:
     * <pre>
     * <code>
     * service
     *      .newKnowledge()
     *      .builder()       // !! important
     *      .newRule()
     *      .forEach("$a", A.class)
     *      .where("$a.active")
     *      .execute()
     *      .build();        // !! important
     * </code>
     * </pre>
     * @return new instance of RuleSetBuilder.
     * @since 3.1.00
     */
    default RuleSetBuilder<C> builder() {
        return builder(getClassLoader());
    }

    /**
     * Sets the activation mode for the session.
     *
     * @param activationMode the activation mode to set
     * @return the updated instance of the class
     */
    C setActivationMode(ActivationMode activationMode);

    /**
     * Configures the {@link TypeResolver} by applying the given action.
     *
     * @param action the action to configure the TypeResolver
     * @return this context
     */
    C configureTypes(Consumer<TypeResolver> action);

    /**
     * Retrieves the ClassLoader used by the current context.
     *
     * @return the ClassLoader used by the object.
     */
    ClassLoader getClassLoader();


    /**
     * Returns the {@link KnowledgeService} instance.
     *
     * @return the {@link KnowledgeService} instance
     */
    KnowledgeService getService();

    /**
     * Returns the Activation Manager factory class for this RuntimeContext.
     *
     * @return the Activation Manager factory class
     */
    Class<? extends ActivationManager> getActivationManagerFactory();

    /**
     * Sets the Activation Manager factory class for this RuntimeContext.
     *
     * @param managerClass the Activation Manager factory class
     * @param <A>          the type of Activation Manager
     */
    <A extends ActivationManager> void setActivationManagerFactory(Class<A> managerClass);

    /**
     * Sets the Activation Manager factory class name for this RuntimeContext.
     *
     * @param managerClass the Activation Manager factory class name
     */
    void setActivationManagerFactory(String managerClass);

    /**
     * Retrieves the TypeResolver instance associated with this RuntimeContext.
     *
     * @return the TypeResolver instance
     */
    TypeResolver getTypeResolver();

    /**
     * Retrieves the Configuration object associated with this RuntimeContext.
     *
     * @return the Configuration object
     */
    Configuration getConfiguration();


    /**
     * Imports all rules using the specified DSL provider and rule source into this ruleset builder.
     *
     * @param provider The DSL provider
     * @param source   Data source containing the rules.
     * @return This RuleSetBuilder instance for method chaining.
     * @throws IOException              If there is an issue reading from the sources.
     * @throws IllegalArgumentException If the source format is not recognized or cannot be processed.
     * @see DSLKnowledgeProvider#appendTo(RuntimeContext, Object) for more details
     */
    @SuppressWarnings("unchecked")
    default C importRules(@NonNull DSLKnowledgeProvider provider, Object source) throws IOException {
        provider.appendTo(this, source);
        return (C) this;
    }

    /**
     * Imports rules using the specified DSL provider and rule sources into this ruleset builder.
     *
     * @param dslName The name of the DSL provider.
     * @param source  Data source that contains rule definitions
     * @return This RuleSetBuilder instance for method chaining.
     * @throws IOException              If there is an issue reading from the sources.
     * @throws IllegalArgumentException If the source format is not recognized or cannot be processed.
     * @throws IllegalStateException    if no or multiple providers were found for the specified DSL identifier.
     * @see DSLKnowledgeProvider#appendTo(RuntimeContext, Object) for more details
     */
    default C importRules(@NonNull String dslName, Object source) throws IOException {
        return importRules(DSLKnowledgeProvider.load(dslName), source);
    }
}
