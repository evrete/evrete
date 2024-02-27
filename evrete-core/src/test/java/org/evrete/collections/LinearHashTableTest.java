package org.evrete.collections;

import org.evrete.classes.TypeA;
import org.junit.jupiter.api.Test;

class LinearHashTableTest {

    @Test
    void locationTest() {

        LinearHashTable<TypeA> hashTable = new LinearHashTable<>(16);

        for (int i = 0; i < 32; i++) {
            TypeA element = new TypeA();

            long location = hashTable.locationFor(element, (e, arg) -> e == arg);
            int hash = LinearHashTable.hash(location);
            assert hash == element.hashCode();
        }
    }
}
