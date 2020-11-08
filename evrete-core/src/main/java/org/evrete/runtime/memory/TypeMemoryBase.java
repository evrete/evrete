package org.evrete.runtime.memory;

import org.evrete.api.ActiveField;
import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;
import org.evrete.api.Type;
import org.evrete.runtime.PlainMemory;
import org.evrete.runtime.RuntimeAware;
import org.evrete.runtime.RuntimeFactImpl;
import org.evrete.runtime.evaluation.AlphaEvaluator;

import java.util.Arrays;

abstract class TypeMemoryBase extends RuntimeAware<SessionMemory> implements BiMemory<TypeMemoryComponent, TypeMemoryComponent>, PlainMemory {
    private final TypeMemoryComponent[] components = new TypeMemoryComponent[MemoryScope.values().length];

    protected ActiveField[] cachedActiveFields;
    protected AlphaEvaluator[] cachedAlphaEvaluators;
    Type<?> type;


    public TypeMemoryBase(SessionMemory runtime, Type<?> type) {
        super(runtime);
        for (MemoryScope scope : MemoryScope.values()) {
            components[scope.ordinal()] = new TypeMemoryComponent(scope);
        }
        this.type = type;
        this.cachedActiveFields = runtime.getActiveFields(type);
        this.cachedAlphaEvaluators = runtime.getAlphaConditions().getPredicates(type).data;
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
        //System.out.println("&&&&& merged");
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

    /**
     * <p>
     * Modifies existing facts by appending value of the newly
     * created field
     * </p>
     *
     * @param newField newly created field
     */
    final void onNewActiveField(ActiveField newField) {
        for (MemoryScope scope : MemoryScope.values()) {
            TypeMemoryComponent component = get(scope);
            ReIterator<RuntimeFact> it = component.iterator();
            while (it.hasNext()) {
                RuntimeFactImpl rto = (RuntimeFactImpl) it.next();
                Object fieldValue = newField.readValue(rto.getDelegate());
                rto.appendValue(newField, fieldValue);
            }

        }
        this.cachedActiveFields = getRuntime().getActiveFields(type);
    }


    @Override
    public String toString() {
        return Arrays.toString(components);
    }
}
