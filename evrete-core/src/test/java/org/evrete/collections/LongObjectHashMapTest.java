package org.evrete.collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LongObjectHashMapTest {

    private LongObjectHashMap<String> map;

    private final Random random = new SecureRandom();

    @BeforeEach
    public void setUp() {
        map = new LongObjectHashMap<>();
    }

    @Test
    public void testPutAndGet() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");

        assertEquals("one", map.get(1));
        assertEquals("two", map.get(2));
        assertEquals("three", map.get(3));

        assertEquals("three", map.put(3, "new three"));
        assertEquals("new three", map.put(3, "new three"));
    }

    @Test
    public void testUpdateValue() {
        map.put(1, "one");
        map.put(1, "new-one");

        assertEquals("new-one", map.get(1));
    }

    @Test
    public void testGetNonExistentKey() {
        assertNull(map.get(999));
    }

    @Test
    public void testRemove() {
        map.put(1, "one");
        map.put(2, "two");

        assertEquals("one", map.get(1));
        assertEquals("one", map.remove(1));
        assertNull(map.get(1));

        assertEquals("two", map.get(2));
        assertEquals("two", map.remove(2));
        assertNull(map.get(2));
    }

    @Test
    public void testRemoveNonExistentKey() {
        map.put(1, "one");
        assertNull(map.remove(999));
        assertEquals("one", map.get(1));
        assertEquals("one", map.remove(1));
    }

    @Test
    public void testSize() {
        assertEquals(0, map.size());
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        assertEquals(3, map.size());

        map.remove(2);
        assertEquals(2, map.size());
        map.remove(99);
        assertEquals(2, map.size());
    }

    @Test
    public void testRehashing() {
        int count = 20000;

        for (int i = 0; i < count; i++) {
            map.put(i, "value" + i);
        }

        for (int i = 0; i < count; i++) {
            assertEquals("value" + i, map.get(i));
        }
        assertEquals(count, map.size());
    }

    @Test
    void iteratorTestReadOps() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(4, "four");

        Iterator<String> iterator = map.iterator();
        assertTrue(iterator.hasNext());
        assertEquals("one", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("two", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("three", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("four", iterator.next());
        assertFalse(iterator.hasNext());

        // Update value
        map.put(3, "new three");
        iterator = map.iterator();
        assertTrue(iterator.hasNext());
        assertEquals("one", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("two", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("new three", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("four", iterator.next());
        assertFalse(iterator.hasNext());


        List<String> values = new LinkedList<>();
        map.forEach(values::add);
        Assertions.assertEquals(List.of("one", "two", "new three", "four"), values);
    }

    @Test
    void randomizedComparison() {
        Map<Long, String> reference = new HashMap<>();
        int count = 4096;
        long range = 2048;

        for (int j = 0; j < 3; j++) {
            // Add random values (2 inserts + 1 delete)
            for (int i = 0; i < count; i++) {
                // Adding two values
                putRandom(range, map, reference);
                putRandom(range, map, reference);
                // And removing one
                deleteRandom(range, map, reference);
                assertSameData(map, reference);
            }

            // Performing the reverse (1 insert, 4 deletes)
            for (int i = 0; i < count; i++) {
                // Adding one value...
                putRandom(range, map, reference);
                // And removing four
                deleteRandom(range, map, reference);
                deleteRandom(range, map, reference);
                deleteRandom(range, map, reference);
                deleteRandom(range, map, reference);
                assertSameData(map, reference);
            }

            map.clear();
            reference.clear();
            assertSameData(map, reference);
        }

    }

    @Test
    void streamsAndIterators() {
        Map<Long, String> reference = new HashMap<>();
        long range = 515;
        for (int round = 0; round < 5; round++) {
            for (int i = 0; i < 100_000; i++) {
                putRandom(range, map, reference);
                putRandom(range, map, reference);
                deleteRandom(range, map, reference);
            }

            Set<String> referenceKeys = new HashSet<>(reference.values());
            Set<String> mapIteratorKeys = new HashSet<>();
            Set<String> mapStreamKeys1 = new HashSet<>();
            Set<String> mapStreamKeys2 = Collections.synchronizedSet(new HashSet<>());
            Set<String> threadNames = Collections.synchronizedSet(new HashSet<>());

            map.forEach(mapIteratorKeys::add);

            Stream<String> stream1 = map.values();
            stream1.forEach(mapStreamKeys1::add);

            Stream<String> stream2 = map.values().parallel();
            stream2.forEach(s -> {
                mapStreamKeys2.add(s);
                threadNames.add(Thread.currentThread().getName());
            });


            Assertions.assertEquals(referenceKeys, mapIteratorKeys);
            Assertions.assertEquals(referenceKeys, mapStreamKeys1);
            Assertions.assertEquals(referenceKeys, mapStreamKeys2);

            if(Runtime.getRuntime().availableProcessors() > 1) {
                assertTrue(threadNames.size() > 1);
            }

            assertSameData(map, reference);
            map.clear();
            reference.clear();
        }

    }

    private static void assertSameData(LongObjectHashMap<String> map, Map<Long, String> reference) {
        Assertions.assertEquals(reference.size(), map.size());
        reference.forEach((k, v) -> Assertions.assertEquals(v, map.get(k)));
    }

    private void putRandom(long range, LongObjectHashMap<String> map, Map<Long, String> reference) {
        long insertKey = random.nextLong(range);
        String value = "Value " + insertKey;

        String expected = reference.put(insertKey, value);
        String actual =  map.put(insertKey, value);
        Assertions.assertEquals(expected, actual, "State: " + map);
    }

    private void deleteRandom(long range, LongObjectHashMap<String> map, Map<Long, String> reference) {
        long deleteKey = random.nextLong(range);
        String expected = reference.remove(deleteKey);
        String actual = map.remove(deleteKey);
        Assertions.assertEquals(expected, actual);
    }

}
