package org.evrete.api;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.Comparator;

/**
 * @param <C> context type parameter
 */
public interface RuntimeContext<C extends RuntimeContext<C>> extends Listeners, FluentImports<RuntimeContext<?>>, PropertyAccess<C> {
    Comparator<Rule> SALIENCE_COMPARATOR = (rule1, rule2) -> -1 * Integer.compare(rule1.getSalience(), rule2.getSalience());

    Comparator<Rule> getRuleComparator();

    void setRuleComparator(Comparator<Rule> comparator);

    RuleBuilder<C> newRule(String name);

    RuleBuilder<C> newRule();

    void wrapTypeResolver(TypeResolverWrapper wrapper);

    C setActivationMode(ActivationMode activationMode);

    ExpressionResolver getExpressionResolver();

    ClassLoader getClassLoader();

    KnowledgeService getService();

    void setClassLoader(ClassLoader classLoader);

    Class<? extends ActivationManager> getActivationManagerFactory();

    <A extends ActivationManager> void setActivationManagerFactory(Class<A> managerClass);

    void setActivationManagerFactory(String managerClass);

    TypeResolver getTypeResolver();

    Configuration getConfiguration();

    C appendDslRules(String dsl, InputStream... streams) throws IOException;

    C appendDslRules(String dsl, URL... resources) throws IOException;

    C appendDslRules(String dsl, Class<?> classes) throws IOException;

    C appendDslRules(String dsl, Reader... readers) throws IOException;

    default C appendDslRules(String dsl, String... sources) throws IOException {
        Reader[] readers = new Reader[sources.length];
        for (int i = 0; i < sources.length; i++) {
            readers[i] = new StringReader(sources[i]);
        }
        return appendDslRules(dsl, readers);
    }
}
