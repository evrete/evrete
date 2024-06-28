package org.evrete.dsl;

import org.evrete.dsl.annotation.EventSubscription;
import org.evrete.dsl.annotation.FieldDeclaration;
import org.evrete.dsl.annotation.Rule;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

class RulesClass extends WrappedClass {

    final List<RuleMethod> ruleMethods = new LinkedList<>();
    final List<WrappedFieldDeclarationMethod<?,?>> fieldDeclarationMethods = new LinkedList<>();
    final List<WrappedEventSubscriptionMethod<?>> subscriptionMethods = new LinkedList<>();

    RulesClass(WrappedClass other) {
        super(other);

        for (Method m : this.publicMethods) {
            Rule ruleAnnotation = m.getAnnotation(Rule.class);
            FieldDeclaration fieldDeclaration = m.getAnnotation(FieldDeclaration.class);
            EventSubscription eventSubscription = m.getAnnotation(EventSubscription.class);
            // The annotations above are mutually exclusive.
            if (ruleAnnotation != null) {
                this.ruleMethods.add(new RuleMethod(this, m, ruleAnnotation));
            } else if (fieldDeclaration != null) {
                this.fieldDeclarationMethods.add(new WrappedFieldDeclarationMethod<>(this, m, fieldDeclaration));
            } else if (eventSubscription != null) {
                this.subscriptionMethods.add(new WrappedEventSubscriptionMethod<>(this, m, eventSubscription.async()));
            }
        }
    }


}
