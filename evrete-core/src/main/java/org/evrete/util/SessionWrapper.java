package org.evrete.util;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public class SessionWrapper implements StatefulSession {
    private final StatefulSession delegate;

    public SessionWrapper(StatefulSession delegate) {
        this.delegate = delegate;
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
    public StatefulSession set(String property, Object value) {
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
    public ActivationManager getActivationManager() {
        return delegate.getActivationManager();
    }

    @Override
    public StatefulSession setActivationManager(ActivationManager activationManager) {
        return delegate.setActivationManager(activationManager);
    }

    @Override
    public void forEachFact(BiConsumer<FactHandle, Object> consumer) {
        delegate.forEachFact(consumer);
    }

    @Override
    public RuntimeContext<?> getParentContext() {
        return delegate.getParentContext();
    }

    @Override
    public void fire() {
        delegate.fire();
    }

    @Override
    public <T> Future<T> fireAsync(T result) {
        return delegate.fireAsync(result);
    }

    @Override
    public Future<?> fireAsync() {
        return delegate.fireAsync();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public List<RuntimeRule> getRules() {
        return delegate.getRules();
    }

    @Override
    public RuntimeRule compileRule(RuleBuilder<?> builder) {
        return delegate.compileRule(builder);
    }

    @Override
    public boolean ruleExists(String name) {
        return delegate.ruleExists(name);
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
    public RuleBuilder<StatefulSession> newRule(String name) {
        return delegate.newRule(name);
    }

    @Override
    public RuleBuilder<StatefulSession> newRule() {
        return delegate.newRule();
    }

    @Override
    public void wrapTypeResolver(TypeResolverWrapper wrapper) {
        delegate.wrapTypeResolver(wrapper);
    }

    @Override
    public StatefulSession setActivationMode(ActivationMode activationMode) {
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
    public StatefulSession setFireCriteria(BooleanSupplier fireCriteria) {
        return delegate.setFireCriteria(fireCriteria);
    }

    @Override
    public RuntimeRule getRule(String name) {
        return delegate.getRule(name);
    }

    @Override
    public RuntimeRule getRule(Named named) {
        return delegate.getRule(named);
    }

    @Override
    public void insertAndFire(Collection<?> objects) {
        delegate.insertAndFire(objects);
    }

    @Override
    public void insertAndFire(Object... objects) {
        delegate.insertAndFire(objects);
    }

    @Override
    public FactHandle insert(Object fact) {
        return delegate.insert(fact);
    }

    @Override
    public Object getFact(FactHandle handle) {
        return delegate.getFact(handle);
    }

    @Override
    public FactHandle insert(String type, Object fact) {
        return delegate.insert(type, fact);
    }

    @Override
    public void update(FactHandle handle, Object newValue) {
        delegate.update(handle, newValue);
    }

    @Override
    public void delete(FactHandle handle) {
        delegate.delete(handle);
    }

    @Override
    public void insert(String type, Collection<?> objects) {
        delegate.insert(type, objects);
    }

    @Override
    public void insert(Collection<?> objects) {
        delegate.insert(objects);
    }

    @Override
    public void insert(Object... objects) {
        delegate.insert(objects);
    }
}
