package org.evrete.jsr94;

import org.evrete.api.StatefulSession;

import javax.rules.RuleExecutionSetMetadata;
import javax.rules.RuleSession;

/**
 * Base class that implements the RuleSession interface. It provides common functionality and fields that can be used by subclasses
 * to implement specific rule sessions.
 */
public abstract class AbstractRuleSessionBase implements RuleSession {
    final StatefulSession delegate;
    private final int type;
    private final RuleExecutionSetMetadataImpl metadata;

    AbstractRuleSessionBase(StatefulSession delegate, int type, RuleExecutionSetMetadataImpl metadata) {
        this.type = type;
        this.delegate = delegate;
        this.metadata = metadata;
    }

    @Override
    public final RuleExecutionSetMetadata getRuleExecutionSetMetadata() {
        return metadata;
    }

    @Override
    public final void release() {
        delegate.close();
    }

    @Override
    public final int getType() {
        return type;
    }
}
