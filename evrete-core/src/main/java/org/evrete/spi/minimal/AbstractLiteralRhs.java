package org.evrete.spi.minimal;

import org.evrete.api.RhsContext;
import org.evrete.api.RuntimeFact;
import org.evrete.api.RuntimeRule;
import org.evrete.runtime.FactType;

import java.util.function.Consumer;

public abstract class AbstractLiteralRhs implements Consumer<RhsContext>, RhsContext {

    private RhsContext current;
    private FactType[] lhsTypes;
    private Class<?>[] varTypes;

    protected abstract void doRhs();

    void setLhsTypes(FactType[] lhsTypes) {
        this.lhsTypes = lhsTypes;

        this.varTypes = new Class<?>[lhsTypes.length];
    }

    protected FactType[] getLhsTypes() {
        return lhsTypes;
    }

    @Override
    public void accept(RhsContext ctx) {
        this.current = ctx;
/*
        for (int i = 0; i < lhsTypes.length; i++) {
            values[i] = ctx.get(lhsTypes[i].getVar());
        }
*/
        doRhs();
    }

    @Override
    public RuntimeFact getFact(String name) {
        return current.getFact(name);
    }

    @Override
    public RhsContext update(Object obj) {
        return current.update(obj);
    }

    @Override
    public RhsContext delete(Object obj) {
        return current.delete(obj);
    }

    @Override
    public RuntimeRule getRule() {
        return current.getRule();
    }

    @Override
    public RhsContext insert(Object obj) {
        return current.insert(obj);
    }
}
