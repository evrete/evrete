package org.evrete.util;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.api.builders.RuleSetBuilder;
import org.evrete.api.events.ContextEvent;
import org.evrete.api.events.Events;
import org.evrete.api.spi.SourceCompiler;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

class RuntimeContextWrapper<D extends RuleSetContext<C, R>, C extends RuntimeContext<C>, R extends Rule> implements RuleSetContext<C, R> {
    protected final D delegate;

    @SuppressWarnings("WeakerAccess")
    protected RuntimeContextWrapper(D delegate) {
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
    public String getName() {
        return delegate.getName();
    }

    @Override
    public final R getRule(String name) {
        return delegate.getRule(name);
    }

    @Override
    public C set(String property, Object value) {
        delegate.set(property, value);
        return self();
    }

    @Override
    public <E extends ContextEvent> Events.Publisher<E> getPublisher(Class<E> eventClass) {
        return delegate.getPublisher(eventClass);
    }

    @Override
    public C configureTypes(Consumer<TypeResolver> action) {
        delegate.configureTypes(action);
        return self();
    }

    @SuppressWarnings("unchecked")
    protected C self() {
        return (C) this;
    }

    @Override
    public C addImport(String imp) {
        delegate.addImport(imp);
        return self();
    }

    @Override
    public final List<R> getRules() {
        return delegate.getRules();
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
    public RuleSetBuilder<C> builder() {
        return delegate.builder();
    }

    @Override
    public C setActivationMode(ActivationMode activationMode) {
        delegate.setActivationMode(activationMode);
        return self();
    }

    @Override
    public EvaluatorsContext getEvaluatorsContext() {
        return delegate.getEvaluatorsContext();
    }

    @Override
    public ClassLoader getClassLoader() {
        return delegate.getClassLoader();
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

    @Override
    public SourceCompiler getSourceCompiler() {
        return delegate.getSourceCompiler();
    }
}
