package org.evrete.jsr94;

import org.evrete.api.StatefulSession;

import javax.rules.RuleExecutionSetMetadata;

class RuleExecutionSetMetadataImpl implements RuleExecutionSetMetadata {
    private static final long serialVersionUID = 4316307421926178732L;
    private final String uri;
    private final StatefulSession delegate;

    RuleExecutionSetMetadataImpl(String uri, StatefulSession delegate) {
        assert uri != null;
        this.uri = uri;
        this.delegate = delegate;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public String getName() {
        return delegate.get(Constants.RULE_SET_NAME, this.uri);
    }

    @Override
    public String getDescription() {
        return delegate.get(Constants.RULE_SET_DESCRIPTION);
    }
}
