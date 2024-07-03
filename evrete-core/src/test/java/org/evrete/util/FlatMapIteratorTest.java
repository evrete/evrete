package org.evrete.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlatMapIteratorTest {

    @Test
    void test1() {
        List<String> list = List.of("aa_", "bb_", "cc");

        Iterator<String> iterator = list.iterator();
        Iterator<Character> mapped = new FlatMapIterator<>(iterator, s -> toChars(s).iterator());

        StringBuilder sb = new StringBuilder();
        mapped.forEachRemaining(sb::append);
        assertEquals("aa_bb_cc", sb.toString());
    }

    @Test
    void test2() {
        List<String> list = List.of("a");

        Iterator<String> iterator = list.iterator();
        Iterator<Character> mapped = new FlatMapIterator<>(iterator, s -> toChars(s).iterator());

        StringBuilder sb = new StringBuilder();
        mapped.forEachRemaining(sb::append);
        assertEquals("a", sb.toString());
    }

    @Test
    void test3() {
        List<String> list = List.of("");

        Iterator<String> iterator = list.iterator();
        Iterator<Character> mapped = new FlatMapIterator<>(iterator, s -> toChars(s).iterator());

        StringBuilder sb = new StringBuilder();
        mapped.forEachRemaining(sb::append);
        assertEquals("", sb.toString());
    }

    @Test
    void test4() {
        List<String> list = List.of("a", "b", "c");

        Iterator<String> iterator = list.iterator();
        Iterator<Character> mapped = new FlatMapIterator<>(iterator, s -> Collections.emptyIterator());

        StringBuilder sb = new StringBuilder();
        mapped.forEachRemaining(sb::append);
        assertEquals("", sb.toString());
    }

    @Test
    void test5() {
        List<String> list = List.of("aa_", "", "cc");

        Iterator<String> iterator = list.iterator();
        Iterator<Character> mapped = new FlatMapIterator<>(iterator, s -> toChars(s).iterator());

        StringBuilder sb = new StringBuilder();
        mapped.forEachRemaining(sb::append);
        assertEquals("aa_cc", sb.toString());
    }


    static List<Character> toChars(String s) {
        char[] chars = s.toCharArray();
        List<Character> list = new ArrayList<>(chars.length);
        for (char c : chars) {
            list.add(c);
        }
        return list;
    }
}
