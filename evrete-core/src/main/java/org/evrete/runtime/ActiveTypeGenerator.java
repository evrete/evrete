package org.evrete.runtime;

import org.evrete.api.Copyable;
import org.evrete.api.Type;
import org.evrete.collections.IndexingArrayMap;

import java.util.function.UnaryOperator;

/**
 * Creates and indexes active representations of declared types.
 * @see ActiveType
 */
public class ActiveTypeGenerator extends IndexingArrayMap<Type<?>, String, ActiveType.Idx, ActiveType> implements Copyable<ActiveTypeGenerator> {

    ActiveTypeGenerator() {
        super(Type::getName);
    }

    @Override
    protected ActiveType.Idx generateKey(Type<?> value, int index) {
        return new ActiveType.Idx(index);
    }

    @Override
    protected ActiveType generateValue(ActiveType.Idx idx, Type<?> value) {
        return new ActiveType(idx, value);
    }

    private ActiveTypeGenerator(ActiveTypeGenerator other, UnaryOperator<ActiveType> copyOp) {
        super(other, copyOp);
    }

    @Override
    public ActiveTypeGenerator copyOf() {
        return new ActiveTypeGenerator(this, ActiveType::copyOf);
    }
}
