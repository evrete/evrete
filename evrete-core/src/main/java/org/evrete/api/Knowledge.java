package org.evrete.api;

import org.evrete.runtime.RuleDescriptor;

import java.util.List;

public interface Knowledge extends RuntimeContext<Knowledge> {
    StatefulSession createSession();

    List<RuleDescriptor> getRuleDescriptors();

    @Override
    Knowledge addImport(String imp);

    @Override
    Knowledge addImport(Class<?> type);

    default <A extends ActivationManager> Knowledge activationManager(Class<A> factory) {
        setActivationManagerFactory(factory);
        return this;
    }

    @Override
    default RuntimeRule deployRule(RuleDescriptor descriptor) {
        throw new UnsupportedOperationException("Rules can not be deployed in knowledge context.");
    }
}
