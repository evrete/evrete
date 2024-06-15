package org.evrete.collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LongObjectHashMapTest {

    private LongObjectHashMap<String> map;

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

}
