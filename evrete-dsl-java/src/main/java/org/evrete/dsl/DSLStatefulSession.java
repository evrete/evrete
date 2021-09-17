package org.evrete.dsl;

import org.evrete.api.*;
import org.evrete.util.SessionWrapperStateful;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;

class DSLStatefulSession extends SessionWrapperStateful {
    private final Listeners listeners;


    DSLStatefulSession(StatefulSession delegate, RulesetMeta meta, FieldDeclarations fieldDeclarations, List<DSLRule> rules, Object classInstance) {
        super(delegate);
        this.listeners = meta.listeners.copy(classInstance);


        fieldDeclarations.applyNormal(getTypeResolver(), classInstance);
        // Adjusting rules for class instance
        for (DSLRule r : rules) {
            RuntimeRule rule = getRule(r.ruleMethod.getRuleName());

            // Replacing RHS
            rule.setRhs(r.ruleMethod.copy(classInstance));

            // Replacing evaluators
            for (PredicateMethod pm : r.predicateMethods) {
                this.replaceEvaluator(pm.handle, pm.copy(classInstance));
            }
        }

        listeners.fire(Phase.CREATE, this);
    }

    @Override
    public void fire() {
        listeners.fire(Phase.FIRE, this);
        super.fire();
    }

    @Override
    public <T> Future<T> fireAsync(T result) {
        listeners.fire(Phase.FIRE, this);
        return super.fireAsync(result);
    }

    @Override
    public void close() {
        listeners.fire(Phase.CLOSE, this);
        super.close();
    }

    @Override
    public StatefulSession set(String property, Object value) {
        super.set(property, value);
        return this;
    }

    @Override
    public RuntimeContext<?> addImport(RuleScope scope, String imp) {
        super.addImport(scope, imp);
        return this;
    }

    @Override
    public StatefulSession setActivationMode(ActivationMode activationMode) {
        super.setActivationMode(activationMode);
        return this;
    }

    @Override
    public StatefulSession setActivationManager(ActivationManager activationManager) {
        super.setActivationManager(activationManager);
        return this;
    }

    @Override
    public StatefulSession setFireCriteria(BooleanSupplier fireCriteria) {
        super.setFireCriteria(fireCriteria);
        return this;
    }


}
