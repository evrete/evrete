package org.evrete.spi.minimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DefaultValueIndexerTest {
    private DefaultValueIndexer<String> indexer;

    @BeforeEach
    void init() {
        this.indexer = new DefaultValueIndexer<>();
    }

    @Test
    void basicUsage() {
        assertState();

        String s1 = "Hello World 1";
        String s2 = "Hello World 2";

        long id1 = indexer.getOrCreateId(s1);
        long id2 = indexer.getOrCreateId(s2);
        assertState();

        Assertions.assertEquals(s1, indexer.get(id1));
        Assertions.assertEquals(s2, indexer.get(id2));

        // Redefine with the same ids
        indexer.assignId(id1, s1);
        indexer.assignId(id2, s2);

        Assertions.assertEquals(s1, indexer.get(id1));
        Assertions.assertEquals(s2, indexer.get(id2));
        assertState();

        // Calling getOrCreate again
        Assertions.assertEquals(id1, indexer.getOrCreateId("Hello World 1"));
        Assertions.assertEquals(id2, indexer.getOrCreateId("Hello World 2"));
        assertState();
    }

    @Test
    void testAutoIndexer() {
        assertState();

        String s1 = "Hello World 1";
        String s2 = "Hello World 2";

        long id1 = 0L;
        long id2 = 1L;
        indexer.assignId(id1, s1);
        indexer.assignId(id2, s2);
        assertState();

        long id3 = indexer.getOrCreateId("Hello World 3");
        assertState();
        Assertions.assertEquals(id3, 2L);

        indexer.assignId(1_000_000, "A million");
        assertState();

        Assertions.assertEquals(indexer.getCounter().get(), 1_000_001L);

        long next = indexer.getOrCreateId("Another Value");
        Assertions.assertEquals(next, 1_000_001L);
        assertState();
    }

    private void assertState() {
        Set<String> values1 = storedValues1();
        Set<String> values2 = storedValues2();
        assertEquals(values1, values2);

        Set<Long> keys1 = storedKeys1();
        Set<Long> keys2 = storedKeys2();
        assertEquals(keys1, keys2);

        assert keys1.size() == values1.size();

    }


    Set<String> storedValues1() {
        return new HashSet<>(indexer.getValueToLong().keySet());
    }

    Set<String> storedValues2() {
        Set<String> values = new HashSet<>();
        indexer.getLongToValue().forEach(values::add);
        return values;
    }

    Set<Long> storedKeys1() {
        return new HashSet<>(indexer.getValueToLong().values());
    }

    Set<Long> storedKeys2() {
        Set<Long> keys = new HashSet<>();
        indexer.getLongToValue().keys().forEach(keys::add);
        return keys;
    }
}
