package org.evrete.runtime.memory;

public class TypeMemoryComponent implements BiMemoryComponent<TypeMemoryComponent> {
    private final IdentityMap map = new IdentityMap();


    @Override
    public void addAll(TypeMemoryComponent other) {
        map.bulkAdd(other.map);
    }

    @Override
    public void clearData() {
        map.clear();
    }
}
