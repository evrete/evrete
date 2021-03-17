package org.evrete.jsr94;

import org.evrete.api.StatefulSession;

import javax.rules.RuleExecutionSetMetadata;
import javax.rules.RuleSession;

public abstract class AbstractRuleSession implements RuleSession {
    final StatefulSession delegate;
    private final int type;
    private final RuleExecutionSetMetadataImpl metadata;

    AbstractRuleSession(StatefulSession delegate, int type, RuleExecutionSetMetadataImpl metadata) {
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
