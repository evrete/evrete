package org.evrete.api;

import org.evrete.runtime.RuleDescriptor;

import java.util.List;

public interface Knowledge extends RuntimeContext<Knowledge> {
    StatefulSession createSession();

    List<RuleDescriptor> getRuleDescriptors();

    @Override
    default RuntimeRule deployRule(RuleDescriptor descriptor) {
        throw new UnsupportedOperationException("Rules can not be deployed in knowledge context.");
    }
}
