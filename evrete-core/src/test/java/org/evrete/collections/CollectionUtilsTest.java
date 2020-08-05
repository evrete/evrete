package org.evrete.collections;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.evrete.util.CollectionUtils.*;


class CollectionUtilsTest {


    @Test
    void permutation1() {
        List<String> test = Arrays.asList("A", "B", "C", "D");
        List<List<String>> result = permutation(test);
        assert result.size() == 24;
        List<String> l = result.get(7);
        assert l.size() == 4;
        assert l.contains("A");
        assert l.contains("B");
        assert l.contains("C");
        assert l.contains("D");
    }


    @Test
    void combinations1() {
        List<String> a = Arrays.asList("a1", "a2", "a3");
        List<String> b = Arrays.asList("b1", "b2");
        List<String> c = Arrays.asList("c1", "c2");
        List<List<String>> ll = Arrays.asList(a, b, c);

        Collection<List<String>> combinations = combinations(ll);
        assert combinations.size() == a.size() * b.size() * c.size();
        assert combinations.iterator().next().size() == ll.size();
    }

    @Test
    void combinations2() {
        List<String> a = Arrays.asList("a1", "a2", "a3");
        List<List<String>> ll = Collections.singletonList(a);

        Collection<List<String>> combinations = combinations(ll);
        assert combinations.size() == a.size();
        assert combinations.iterator().next().size() == ll.size();

    }

    @Test
    void systemFill1() {
        String[] arr = new String[10];
        Arrays.fill(arr, "a");

        systemFill(arr, "b");
        Set<String> set = Arrays.stream(arr).collect(Collectors.toSet());
        assert set.size() == 1;
        assert set.contains("b");
    }
}
