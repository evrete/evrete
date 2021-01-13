package org.evrete.runtime.memory;

import org.evrete.api.Type;
import org.evrete.runtime.ActiveField;
import org.evrete.runtime.RuntimeAware;
import org.evrete.runtime.evaluation.AlphaEvaluator;

abstract class TypeMemoryBase extends RuntimeAware<SessionMemory> {
    protected ActiveField[] cachedActiveFields;
    protected AlphaEvaluator[] cachedAlphaEvaluators;
    final Type<?> type;

    public TypeMemoryBase(SessionMemory runtime, Type<?> type) {
        super(runtime);
        this.type = type;
        this.cachedActiveFields = runtime.getActiveFields(type);
        this.cachedAlphaEvaluators = runtime.getAlphaConditions().getPredicates(type).data;
    }

    public Type<?> getType() {
        return type;
    }


}
