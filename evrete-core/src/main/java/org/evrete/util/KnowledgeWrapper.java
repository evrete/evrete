package org.evrete.util;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.runtime.RuleDescriptor;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class KnowledgeWrapper implements Knowledge {
    private final Knowledge delegate;

    public KnowledgeWrapper(Knowledge delegate) {
        this.delegate = delegate;
    }

    @Override
    public Collection<RuleSession<?>> getSessions() {
        return delegate.getSessions();
    }

    @Override
    public StatefulSession createSession() {
        return delegate.createSession();
    }

    @Override
    public <A extends ActivationManager> Knowledge activationManager(Class<A> factory) {
        return delegate.activationManager(factory);
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
    public RuleBuilder<Knowledge> newRule(String name) {
        return delegate.newRule(name);
    }

    @Override
    public RuleBuilder<Knowledge> newRule() {
        return delegate.newRule();
    }

    @Override
    public void wrapTypeResolver(TypeResolverWrapper wrapper) {
        delegate.wrapTypeResolver(wrapper);
    }

    @Override
    public Knowledge setActivationMode(ActivationMode activationMode) {
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

    @Override
    public void addListener(EvaluationListener listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeListener(EvaluationListener listener) {
        delegate.removeListener(listener);
    }

    @Override
    public RuntimeContext<?> addImport(RuleScope scope, String imp) {
        return delegate.addImport(scope, imp);
    }

    @Override
    public RuntimeContext<?> addImport(RuleScope scope, Class<?> type) {
        return delegate.addImport(scope, type);
    }

    @Override
    public Set<String> getImports(RuleScope... scopes) {
        return delegate.getImports(scopes);
    }

    @Override
    public Knowledge set(String property, Object value) {
        return delegate.set(property, value);
    }

    @Override
    public <T> T get(String property) {
        return delegate.get(property);
    }

    @Override
    public <T> T get(String name, T defaultValue) {
        return delegate.get(name, defaultValue);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return delegate.getPropertyNames();
    }

    @Override
    public List<RuleDescriptor> getRules() {
        return delegate.getRules();
    }

    @Override
    public RuleDescriptor compileRule(RuleBuilder<?> builder) {
        return delegate.compileRule(builder);
    }

    @Override
    public boolean ruleExists(String name) {
        return delegate.ruleExists(name);
    }
}
