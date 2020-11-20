package org.evrete.runtime.memory;

import org.evrete.api.ActiveField;
import org.evrete.api.Type;
import org.evrete.runtime.RuntimeAware;
import org.evrete.runtime.evaluation.AlphaEvaluator;

abstract class TypeMemoryBase extends RuntimeAware<SessionMemory> {
    //private final TypeMemoryComponent[] components = new TypeMemoryComponent[MemoryScope.values().length];

    protected ActiveField[] cachedActiveFields;
    protected AlphaEvaluator[] cachedAlphaEvaluators;
    Type<?> type;


    public TypeMemoryBase(SessionMemory runtime, Type<?> type) {
        super(runtime);
/*
        for (MemoryScope scope : MemoryScope.values()) {
            components[scope.ordinal()] = new TypeMemoryComponent(scope);
        }
*/
        this.type = type;
        this.cachedActiveFields = runtime.getActiveFields(type);
        this.cachedAlphaEvaluators = runtime.getAlphaConditions().getPredicates(type).data;
    }

    public Type<?> getType() {
        return type;
    }


}
