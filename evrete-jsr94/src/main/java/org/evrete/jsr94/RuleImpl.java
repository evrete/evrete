package org.evrete.jsr94;

import javax.rules.admin.Rule;
import java.io.IOException;

class RuleImpl implements Rule {
    private static final long serialVersionUID = -3889264933101103219L;
    private final org.evrete.api.Rule delegate;

    RuleImpl(org.evrete.api.Rule delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getDescription() {
        return delegate.get(Const.RULE_DESCRIPTION, "");
    }

    @Override
    public Object getProperty(Object o) {
        return delegate.get(o.toString());
    }

    @Override
    public void setProperty(Object o, Object o1) {
        delegate.set(o.toString(), o1);
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        throw new UnsupportedOperationException("Serialization not supported");
    }

}
