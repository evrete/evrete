package org.evrete.spi.minimal;

import org.evrete.api.FactHandle;
import org.evrete.api.RhsContext;
import org.evrete.api.RuntimeRule;

class RhsContextWrapper implements RhsContext {
    private RhsContext ctx;

    public RhsContext getCtx() {
        return ctx;
    }

    public void setCtx(RhsContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public RhsContext update(Object obj) {
        return ctx.update(obj);
    }

    @Override
    public RhsContext delete(Object obj) {
        return ctx.delete(obj);
    }

    @Override
    public RuntimeRule getRule() {
        return ctx.getRule();
    }


    @Override
    public Object getObject(String name) {
        return ctx.getObject(name);
    }

    @Override
    public FactHandle insert0(Object fact, boolean resolveCollections) {
        return ctx.insert0(fact, resolveCollections);
    }

    @Override
    public FactHandle insert0(String type, Object fact, boolean resolveCollections) {
        return ctx.insert0(type, fact, resolveCollections);
    }

    @Override
    public void delete(FactHandle handle) {
        ctx.delete(handle);
    }

    @Override
    public void update(FactHandle handle, Object newValue) {
        ctx.update(handle, newValue);
    }
}
