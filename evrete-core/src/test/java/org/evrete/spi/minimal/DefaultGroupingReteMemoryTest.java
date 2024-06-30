package org.evrete.spi.minimal;

import org.evrete.api.spi.MemoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class DefaultGroupingReteMemoryTest {
    DefaultGroupingReteMemory<String> memory;

    @BeforeEach
    void beforeEach() {
        memory = new DefaultGroupingReteMemory<>();
    }

    Set<Long> keys(MemoryScope scope) {
        Set<Long> result = new HashSet<>();
        memory.iterator(scope).forEachRemaining(result::add);
        return result;
    }

    Set<String> stream(MemoryScope scope, Long key, Long... other) {
        Set<String> result = new HashSet<>();
        memory.valueIterator(scope, key).forEachRemaining(result::add);
        if (other != null) {
            for (Long k : other) {
                memory.valueIterator(scope, k).forEachRemaining(result::add);
            }
        }
        return result;
    }

    @Test
    void insertToEmptyMemory() {

        long KEY_1 = 1;
        long KEY_2 = 2;
        String VALUE_1_1 = "one";
        String VALUE_2_1 = "two 1";
        String VALUE_2_2 = "two 2";

        memory.insert(KEY_1, VALUE_1_1);
        memory.insert(KEY_2, VALUE_2_1);
        memory.insert(KEY_2, VALUE_2_2);


        // Testing keys
        Set<Long> keys = keys(MemoryScope.DELTA);

        assert keys.size() == 2;
        assert keys.contains(KEY_1);
        assert keys.contains(KEY_2);

        // Testing main values
        assert memory.getMain().isEmpty();

        // Testing delta values 1
        Set<String> deltaValues1 = stream(MemoryScope.DELTA, 1L);
        assert deltaValues1.contains(VALUE_1_1);
        assert deltaValues1.size() == 1;

        // Testing delta values 2
        Set<String> deltaValues2 = stream(MemoryScope.DELTA, 2L);
        assert deltaValues2.contains(VALUE_2_1);
        assert deltaValues2.contains(VALUE_2_2);
        assert deltaValues2.size() == 2;

        //
        // Commit & test committed data
        //

        memory.commit();

        // Delta memory must be empty
        assert memory.getDelta().isEmpty();

        // Testing main keys
        Set<Long> mainKeys = keys(MemoryScope.MAIN);
        assert mainKeys.size() == 2 : mainKeys.size();
        assert mainKeys.contains(KEY_1);
        assert mainKeys.contains(KEY_2);

        // Testing main values
        Set<String> mainValues1 = stream(MemoryScope.MAIN, 1L);
        assert mainValues1.contains(VALUE_1_1);
        assert mainValues1.size() == 1;

        Set<String> mainValues2 = stream(MemoryScope.MAIN, 2L);
        assert mainValues2.contains(VALUE_2_1);
        assert mainValues2.contains(VALUE_2_2);
        assert mainValues2.size() == 2;


    }

    @Test
    void insertToExistingMemoryDifferentKeys() {
        long PRE_KEY_1 = -1;
        long PRE_KEY_2 = -2;
        String PRE_VALUE_1_1 = "pre one 1";
        String PRE_VALUE_1_2 = "pre one 2";
        String PRE_VALUE_2_1 = "pre two 1";
        String PRE_VALUE_2_2 = "pre two 2";

        long KEY_1 = 1;
        long KEY_2 = 2;
        String VALUE_1_1 = "post one";
        String VALUE_2_1 = "post two 1";
        String VALUE_2_2 = "post two 2";

        // Insert & commit some data first
        memory.insert(PRE_KEY_1, PRE_VALUE_1_1);
        memory.insert(PRE_KEY_1, PRE_VALUE_1_2);
        memory.insert(PRE_KEY_2, PRE_VALUE_2_1);
        memory.insert(PRE_KEY_2, PRE_VALUE_2_2);
        memory.commit();

        //
        // Insert new entries (different keys)
        //
        memory.insert(KEY_1, VALUE_1_1);
        memory.insert(KEY_2, VALUE_2_1);
        memory.insert(KEY_2, VALUE_2_2);

        // 1. Testing main keys after the insert
        Set<Long> mainKeysPre = keys(MemoryScope.MAIN);

        Assertions.assertEquals(mainKeysPre.size(), 2);
        assert mainKeysPre.contains(PRE_KEY_1);
        assert mainKeysPre.contains(PRE_KEY_2);
        assert !mainKeysPre.contains(KEY_1);
        assert !mainKeysPre.contains(KEY_2);

        // 2. Testing main values after the insert
        Set<String> mainValues1 = stream(MemoryScope.MAIN, KEY_1);
        Set<String> mainValues2 = stream(MemoryScope.MAIN, KEY_2);
        // The newly inserted values are not in the main memory
        assert mainValues1.isEmpty() && mainValues2.isEmpty();

        Set<String> mainValuesPre1 = stream(MemoryScope.MAIN, PRE_KEY_1);
        assert mainValuesPre1.contains(PRE_VALUE_1_1);
        assert mainValuesPre1.contains(PRE_VALUE_1_2);
        assert mainValuesPre1.size() == 2;

        Set<String> mainValuesPre2 = stream(MemoryScope.MAIN, PRE_KEY_2);
        assert mainValuesPre2.contains(PRE_VALUE_2_1);
        assert mainValuesPre2.contains(PRE_VALUE_2_2);
        assert mainValuesPre2.size() == 2;

        // 3. Testing delta values after the insert
        Set<String> deltaValuesPre1 = stream(MemoryScope.DELTA, PRE_KEY_1);
        Set<String> deltaValuesPre2 = stream(MemoryScope.DELTA, PRE_KEY_2);
        // The old values must now show up in the delta scope
        assert deltaValuesPre1.isEmpty() && deltaValuesPre2.isEmpty();

        // The new values must be present in the delta scope
        Set<String> deltaValues1 = stream(MemoryScope.DELTA, KEY_1);
        assert deltaValues1.contains(VALUE_1_1);
        assert deltaValues1.size() == 1;

        Set<String> deltaValues2 = stream(MemoryScope.DELTA, KEY_2);
        assert deltaValues2.contains(VALUE_2_1);
        assert deltaValues2.contains(VALUE_2_2);
        assert deltaValues2.size() == 2;

        //
        // Secondary commit
        //
        memory.commit();

        // The delta memory must be empty
        assert memory.getDelta().isEmpty();

        // Testing main keys after secondary commit
        Set<Long> mainKeysPost = keys(MemoryScope.MAIN);
        assert mainKeysPost.size() == 4;
        assert mainKeysPost.contains(PRE_KEY_1);
        assert mainKeysPost.contains(PRE_KEY_2);
        assert mainKeysPost.contains(KEY_1);
        assert mainKeysPost.contains(KEY_2);

        // Testing main values after secondary commit
        assert stream(MemoryScope.MAIN, KEY_1).contains(VALUE_1_1);
        assert stream(MemoryScope.MAIN, KEY_2).containsAll(List.of(VALUE_2_1, VALUE_2_2));
        assert stream(MemoryScope.MAIN, PRE_KEY_1).containsAll(List.of(PRE_VALUE_1_1, PRE_VALUE_1_2));
        assert stream(MemoryScope.MAIN, PRE_KEY_2).containsAll(List.of(PRE_VALUE_2_1, PRE_VALUE_2_2));

        assert stream(MemoryScope.MAIN, KEY_1, KEY_2, PRE_KEY_1, PRE_KEY_2).size() == 7;


    }

    @Test
    void insertToExistingMemorySameKey1() {
        int PRE_KEY_1 = -1;
        int SAME_KEY_2 = -2;
        String PRE_VALUE_1_1 = "pre one 1";
        String PRE_VALUE_1_2 = "pre one 2";
        String PRE_VALUE_2_1 = "pre two 1";
        String PRE_VALUE_2_2 = "pre two 2";


        // Insert & commit some data first
        memory.insert(PRE_KEY_1, PRE_VALUE_1_1);
        memory.insert(PRE_KEY_1, PRE_VALUE_1_2);
        memory.insert(SAME_KEY_2, PRE_VALUE_2_1);
        memory.insert(SAME_KEY_2, PRE_VALUE_2_2);
        memory.commit();

        //
        // Insert another new entry with the same key
        //
        String POST_VALUE_1 = "post one";
        String POST_VALUE_2 = "post two";
        int KEY_1 = 1;
        memory.insert(KEY_1, POST_VALUE_1);
        memory.insert(SAME_KEY_2, POST_VALUE_2);

        // Test keys
        Set<Long> tmp1 = keys(MemoryScope.DELTA);
        assert tmp1.size() == 2;
    }
}
