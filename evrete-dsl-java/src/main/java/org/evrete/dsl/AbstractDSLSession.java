package org.evrete.dsl;

import org.evrete.api.ActivationManager;
import org.evrete.api.RuleSession;
import org.evrete.api.RuntimeRule;
import org.evrete.util.AbstractSessionWrapper;

import java.util.List;

abstract class AbstractDSLSession<S extends RuleSession<S>> extends AbstractSessionWrapper<S> {
    private final Listeners listeners;


    AbstractDSLSession(S delegate, RulesetMeta meta, FieldDeclarations fieldDeclarations, List<DSLRule> rules, Object classInstance) {
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


        delegate.addEventListener(evt -> {
            switch (evt) {
                case PRE_FIRE:
                    listeners.fire(Phase.FIRE, AbstractDSLSession.this);
                    break;
                case PRE_CLOSE:
                    listeners.fire(Phase.CLOSE, AbstractDSLSession.this);
                    break;
                default:
            }
        });

        if (classInstance instanceof ActivationManager) {
            setActivationManager((ActivationManager) classInstance);
        }

        listeners.fire(Phase.CREATE, this);

    }

}
