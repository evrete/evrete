package org.evrete.dsl;

import org.evrete.api.Environment;

import java.util.Collection;

class MaskedEnvironment implements Environment {
    private final Environment delegate;

    MaskedEnvironment(Environment delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object set(String property, Object value) {
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
}
