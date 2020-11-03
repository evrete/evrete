package org.evrete.runtime.memory;

import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;
import org.evrete.runtime.PlainMemory;
import org.evrete.runtime.RuntimeAware;

abstract class TypeMemoryBase extends RuntimeAware<SessionMemory> implements BiMemory<TypeMemoryComponent, TypeMemoryComponent>, PlainMemory {
    private final TypeMemoryComponent[] components = new TypeMemoryComponent[MemoryScope.values().length];


    public TypeMemoryBase(SessionMemory runtime) {
        super(runtime);
        for (MemoryScope scope : MemoryScope.values()) {
            components[scope.ordinal()] = new TypeMemoryComponent();
        }
    }

    @Override
    public final TypeMemoryComponent get(MemoryScope scope) {
        return components[scope.ordinal()];
    }

    public final long getTotalFacts() {
        return components[MemoryScope.MAIN.ordinal()].totalFacts() + components[MemoryScope.DELTA.ordinal()].totalFacts();
    }


    @Override
    public final void mergeDelta1() {
        TypeMemoryComponent delta = get(MemoryScope.DELTA);
        TypeMemoryComponent main = get(MemoryScope.MAIN);
        main.addAll(delta);
        delta.clearData();
    }

    @Override
    public final ReIterator<RuntimeFact> mainIterator() {
        return components[MemoryScope.MAIN.ordinal()].iterator();
    }

    @Override
    public final ReIterator<RuntimeFact> deltaIterator() {
        return components[MemoryScope.DELTA.ordinal()].iterator();
    }

    @Override
    public final boolean hasChanges() {
        return components[MemoryScope.DELTA.ordinal()].hasData();
    }
}
