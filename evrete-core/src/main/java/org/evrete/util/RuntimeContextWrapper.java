package org.evrete.util;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

public class RuntimeContextWrapper<C extends RuntimeContext<C>> implements RuntimeContext<C> {
    final C delegate;

    protected RuntimeContextWrapper(C delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> T get(String property) {
        return delegate.get(property);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return delegate.getPropertyNames();
    }

    @Override
    public void addListener(EvaluationListener listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeListener(EvaluationListener listener) {
        delegate.removeListener(listener);
    }

    @Override
    public C set(String property, Object value) {
        return delegate.set(property, value);
    }

    @Override
    public RuntimeContext<?> addImport(RuleScope scope, String imp) {
        return delegate.addImport(scope, imp);
    }

    @Override
    public Set<String> getImports(RuleScope... scopes) {
        return delegate.getImports(scopes);
    }

    @Override
    public Comparator<Rule> getRuleComparator() {
        return delegate.getRuleComparator();
    }

    @Override
    public void setRuleComparator(Comparator<Rule> comparator) {
        delegate.setRuleComparator(comparator);
    }

    @Override
    public RuleBuilder<C> newRule(String name) {
        return delegate.newRule(name);
    }

    @Override
    public RuleBuilder<C> newRule() {
        return delegate.newRule();
    }

    @Override
    public void wrapTypeResolver(TypeResolverWrapper wrapper) {
        delegate.wrapTypeResolver(wrapper);
    }

    @Override
    public C setActivationMode(ActivationMode activationMode) {
        return delegate.setActivationMode(activationMode);
    }

    @Override
    public ExpressionResolver getExpressionResolver() {
        return delegate.getExpressionResolver();
    }

    @Override
    public ClassLoader getClassLoader() {
        return delegate.getClassLoader();
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        delegate.setClassLoader(classLoader);
    }

    @Override
    public KnowledgeService getService() {
        return delegate.getService();
    }

    @Override
    public Class<? extends ActivationManager> getActivationManagerFactory() {
        return delegate.getActivationManagerFactory();
    }

    @Override
    public <A extends ActivationManager> void setActivationManagerFactory(Class<A> managerClass) {
        delegate.setActivationManagerFactory(managerClass);
    }

    @Override
    public void setActivationManagerFactory(String managerClass) {
        delegate.setActivationManagerFactory(managerClass);
    }

    @Override
    public TypeResolver getTypeResolver() {
        return delegate.getTypeResolver();
    }

    @Override
    public Configuration getConfiguration() {
        return delegate.getConfiguration();
    }
}
