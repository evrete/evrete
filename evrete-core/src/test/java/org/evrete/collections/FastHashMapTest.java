package org.evrete.collections;

import org.evrete.classes.TypeA;
import org.evrete.helper.Mapping;
import org.evrete.helper.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.*;

@SuppressWarnings("AssertWithSideEffects")
class FastHashMapTest {

    private static void mapFill(Collection<String> data, Mapping<String, String> fastMap, Mapping<String, String> hashMap) {
        for (String s : data) {
            String b1 = fastMap.put(s, s);
            String b2 = hashMap.put(s, s);
            assert fastMap.size() == hashMap.size() : "Fast: " + fastMap.size() + ", Hash: " + hashMap.size();
            assert Objects.equals(b1, b2) : "b1=" + b1 + ", b2=" + b2;
        }
    }

    @SuppressWarnings("unchecked")
    private static void mapRemove(Collection<String> data, Mapping<String, String> fastMap, Mapping<String, String> hashMap) {
        for (String s : data) {

            String r1 = fastMap.remove(s);
            String r2 = hashMap.remove(s);
            assert Objects.equals(r1, r2);
            if (fastMap instanceof LinearHashMap) {
                ((LinearHashMap<String, String>) fastMap).assertStructure();
            }
            assert fastMap.size() == hashMap.size() : "Fast: " + fastMap.size() + ", Hash: " + hashMap.size();
        }
    }

    @Test
    void basic1() {
        LinearHashMap<String, TypeA> map = new LinearHashMap<>(128);

        TypeA a1 = new TypeA("a1");
        TypeA a2 = new TypeA("a2");
        TypeA a3 = new TypeA("a3");
        TypeA a4 = new TypeA("a4");

        assert map.put("1", a1) == null;
        assert map.get("1") == a1 : "Actual: " + map.get("1");
        assert map.size() == 1 : "Actual:" + map.size();
        assert map.put("1", a2) == a1;
        TypeA prev = map.get("1");
        assert prev == a2 : "Actual: " + prev;
        map.assertStructure();

        assert map.size() == 1;
        assert map.put("2", a2) == null;
        map.assertStructure();

        assert map.size() == 2;
        assert map.put("3", a3) == null;
        map.assertStructure();

        assert map.size() == 3;
        assert map.put("4", a4) == null;
        map.assertStructure();
        assert map.size() == 4;

        map.assertStructure();

        Set<String> keys = new HashSet<>();
        Set<TypeA> values = new HashSet<>();

        map.forEachEntry((key, value) -> {
            keys.add(key);
            values.add(value);
        });

        assert keys.size() == map.size();
        assert keys.containsAll(Arrays.asList("1", "2", "3", "4"));
        assert values.size() == map.size() - 1 : "Values: " + values;
        assert values.containsAll(Arrays.asList(a2, a3, a4));
    }

    @Test
    void basic2() {
        LinearHashMap<String, TypeA> map = new LinearHashMap<>(2);

        TypeA a = new TypeA();

        TypeA old;
        String key = "1";
        for (int i = 0; i < 640; i++) {
            old = map.put(key, a);
            assert map.size() == 1;
            assert old == null;
            map.assertStructure();
            old = map.remove(key);
            assert old == a : "Actual:" + old;
            assert map.size() == 0;
            map.assertStructure();
        }

    }

    @Test
    void sizeTest() {
        LinearHashSet<String> set1 = new LinearHashSet<>(5);
        assert set1.dataSize() == 8;

        LinearHashSet<String> set2 = new LinearHashSet<>(0);
        assert set2.dataSize() == 2;

        LinearHashSet<String> set3 = new LinearHashSet<>(32);
        assert set3.dataSize() == 32;
    }


    @Test
    void remove3() {
        Mapping<String, String> fastSet = TestUtils.map(new LinearHashMap<>(2));
        Mapping<String, String> hashSet = TestUtils.map(new HashMap<>(2));

        // Fill first
        int totalEntries = 4096;
        Collection<String> data = new ArrayList<>(totalEntries);
        Random r = new Random(System.nanoTime());
        int max = 4096;
        for (int i = 0; i < totalEntries; i++) {
            data.add(String.valueOf(r.nextInt(max)));
        }

        // Validate fill
        mapFill(data, fastSet, hashSet);
        // Validate remove
        mapRemove(data, fastSet, hashSet);
        assert fastSet.size() == 0;
        assert hashSet.size() == 0;
        // Validate fill again
        mapFill(data, fastSet, hashSet);
        assert fastSet.size() == new HashSet<>(data).size();

    }

}