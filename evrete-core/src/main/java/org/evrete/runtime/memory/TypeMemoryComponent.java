package org.evrete.runtime.memory;

import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;
import org.evrete.runtime.RuntimeFactImpl;

import java.util.function.Consumer;

public class TypeMemoryComponent implements BiMemoryComponent<TypeMemoryComponent> {
    private final IdentityMap map = new IdentityMap();
    private final MemoryScope scope;

    public TypeMemoryComponent(MemoryScope scope) {
        this.scope = scope;
    }

    @Override
    public void addAll(TypeMemoryComponent other) {
        map.bulkAdd(other.map);
    }

    @Override
    public void clearData() {
        map.clear();
    }

    public ReIterator<RuntimeFact> iterator() {
        return map.factIterator();
    }

    public boolean contains(Object o) {
        return map.contains(o);
    }

    //TODO use totalFacts() instead
    public boolean hasData() {
/*
        System.out.println("@@@@@@ " + scope + " -> " + map);
        try {
            throw new Exception();
        } catch (Exception e) {
            e.printStackTrace();
        }
*/
        return map.size() > 0;
    }

    public int totalFacts() {
        return map.size();
    }

    public RuntimeFact remove(Object key) {
        return map.remove(key);
    }

    public void put(Object key, RuntimeFactImpl value) {
        map.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public final <T> void forEachMemoryObject(Consumer<T> consumer) {
        map.forEachKey(f -> consumer.accept((T) f));
    }

    public final void forEachObjectUnchecked(Consumer<Object> consumer) {
        map.forEachKey(consumer);
    }

    @Override
    public String toString() {
        return "{scope=" + scope +
                ", map=" + map +
                '}';
    }
}
