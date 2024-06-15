package org.evrete.spi.minimal;

import org.evrete.api.RhsContext;

import java.util.function.Consumer;

public abstract class AbstractLiteralRhs extends RhsContextWrapper implements Consumer<RhsContext>, RhsContext {

//    @SuppressWarnings("WeakerAccess")
    protected abstract void doRhs();

    @Override
    public void accept(RhsContext ctx) {
        this.setCtx(ctx);
        doRhs();
    }
}
