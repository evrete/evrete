package org.evrete.util;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.*;

import java.util.Collection;
import java.util.Comparator;

public class RuntimeContextWrapper<C extends RuntimeContext<C>> implements RuntimeContext<C> {
    protected final C delegate;

    @SuppressWarnings("WeakerAccess")
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
    @SuppressWarnings("unchecked")
    public C set(String property, Object value) {
        delegate.set(property, value);
        return (C) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C addImport(RuleScope scope, String imp) {
        delegate.addImport(scope, imp);
        return (C) this;
    }

    @Override
    public Imports getImports() {
        return delegate.getImports();
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
    @SuppressWarnings("unchecked")
    public C setActivationMode(ActivationMode activationMode) {
        delegate.setActivationMode(activationMode);
        return (C) this;
    }

    @Override
    public EvaluatorHandle addEvaluator(Evaluator evaluator, double complexity) {
        return delegate.addEvaluator(evaluator, complexity);
    }

    @Override
    public void replaceEvaluator(EvaluatorHandle handle, Evaluator newEvaluator) {
        delegate.replaceEvaluator(handle, newEvaluator);
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
