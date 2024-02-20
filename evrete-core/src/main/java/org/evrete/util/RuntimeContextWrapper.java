package org.evrete.util;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.api.builders.RuleSetBuilder;
import org.evrete.runtime.compiler.CompilationException;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class RuntimeContextWrapper<D extends RuleSetContext<C, R>, C extends RuntimeContext<C>, R extends Rule> implements RuleSetContext<C, R> {
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
    public void addListener(EvaluationListener listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeListener(EvaluationListener listener) {
        delegate.removeListener(listener);
    }

    @Override
    public LiteralEvaluator compile(LiteralExpression expression) throws CompilationException {
        return delegate.compile(expression);
    }

    @Override
    public final FieldReference[] resolveFieldReferences(String[] args, NamedType.Resolver typeMapper) {
        return delegate.resolveFieldReferences(args, typeMapper);
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
    public void setClassLoader(ClassLoader classLoader) {
        delegate.setClassLoader(classLoader);
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
    @Deprecated
    public RuleBuilder<C> newRule(String name) {
        return delegate.newRule(name);
    }

    @Override
    @Deprecated
    public RuleBuilder<C> newRule() {
        return delegate.newRule();
    }

    @Override
    public RuleSetBuilder<C> builder() {
        return delegate.builder();
    }

    @Override
    public void wrapTypeResolver(TypeResolverWrapper wrapper) {
        delegate.wrapTypeResolver(wrapper);
    }

    @Override
    public C setActivationMode(ActivationMode activationMode) {
        delegate.setActivationMode(activationMode);
        return self();
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
    public Evaluator getEvaluator(EvaluatorHandle handle) {
        return delegate.getEvaluator(handle);
    }

    @Override
    public void replaceEvaluator(EvaluatorHandle handle, ValuesPredicate predicate) {
        delegate.replaceEvaluator(handle, predicate);
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
    public JavaSourceCompiler getSourceCompiler() {
        return delegate.getSourceCompiler();
    }
}
