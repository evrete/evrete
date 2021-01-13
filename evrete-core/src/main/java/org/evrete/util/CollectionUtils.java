package org.evrete.util;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class CollectionUtils {

    @SuppressWarnings("unchecked")
    public static <T, Z extends T> Z[] filter(Class<Z> type, T[] input, Predicate<T> predicate) {
        List<Z> list = Arrays
                .stream(input)
                .filter(predicate).map(t -> (Z) t)
                .collect(Collectors.toList());
        return list.toArray(array(type, 0));
    }

    public static <T> boolean deleteFrom(Collection<T> collection, Predicate<T> predicate) {
        LinkedList<T> selected = new LinkedList<>();
        for (T obj : collection) {
            if (predicate.test(obj)) selected.add(obj);
        }

        if (selected.isEmpty()) {
            return false;
        } else {
            for (T o : selected) {
                collection.remove(o);
            }
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] array(Class<T> type, int size) {
        return (T[]) Array.newInstance(type, size);
    }

    public static <E> List<List<E>> permutation(List<E> l) {
        ArrayList<E> original = new ArrayList<>(l); // ArrayList supports remove
        if (original.size() == 0) {
            List<List<E>> result = new ArrayList<>();
            result.add(new ArrayList<>());
            return result;
        }
        E first = original.remove(0);
        List<List<E>> result = new ArrayList<>();
        List<List<E>> permutations = permutation(original);
        for (List<E> list : permutations) {
            for (int i = 0; i <= list.size(); i++) {
                List<E> temp = new ArrayList<>(list);
                temp.add(i, first);
                result.add(temp);
            }
        }
        return result;
    }


    /**
     * For argument like [[a1, a2, a3], [b1, b2], [c1, c2]] method returns collection
     * of size L = a.size() * b.size() * c.size() = 12 where each entry is a combination of the three
     * input sources: [a1, b1 ,c1], [a1, b1, c2], [a1, b2, c1] .... [a3, b3, c2]
     *
     * @param sources input collections
     * @param <E>     type parameter
     * @return all possible combinations
     */
    public static <E, T extends Collection<E>> Collection<List<E>> combinations(Collection<T> sources) {
        Map<Integer, T> mapper = new LinkedHashMap<>();
        int counter = 0;
        for (T collection : sources) {
            mapper.put(counter++, collection);
        }
        List<Map<Integer, E>> combinations = combinations(mapper, LinkedHashMap::new);

        Collection<List<E>> result = new LinkedList<>();

        for (Map<Integer, E> m : combinations) {
            List<E> l = new LinkedList<>(m.values());
            result.add(l);

        }
        return result;
    }

    public static <K, V, C extends Collection<V>> List<Map<K, V>> combinations(Map<K, C> sources, Supplier<Map<K, V>> emptyMapSupplier) {
        Iterator<Map.Entry<K, C>> it = sources.entrySet().iterator();
        if (!it.hasNext()) return Collections.emptyList();

        Map.Entry<K, C> rootCollection = it.next();
        C c = rootCollection.getValue();
        K k = rootCollection.getKey();
        List<Map<K, V>> response = new ArrayList<>(c.size());
        for (V o : c) {
            Map<K, V> m = emptyMapSupplier.get();
            m.put(k, o);
            response.add(m);
        }

        while (it.hasNext()) {
            rootCollection = it.next();
            c = rootCollection.getValue();
            k = rootCollection.getKey();
            if (!c.isEmpty()) {
                List<Map<K, V>> newResponse = new ArrayList<>(c.size() * response.size());
                for (V e : c) {
                    for (Map<K, V> responseEntry : response) {
                        Map<K, V> newResponseEntry = emptyMapSupplier.get();
                        newResponseEntry.putAll(responseEntry);
                        newResponseEntry.put(k, e);
                        newResponse.add(newResponseEntry);
                    }
                }
                response = newResponse;
            }
        }
        return response;
    }

    public static void systemFill(int[] array, int value) {
        systemFill(array, 0, array.length, value);
    }

    public static void systemFill(boolean[] array, boolean value) {
        systemFill(array, 0, array.length, value);
    }

    public static <T> void systemFill(T[] array, T value) {
        systemFill(array, 0, array.length, value);
    }

    /**
     * This method is an alternative to Arrays.fill() with the same
     * method signature
     *
     * @param array     the array to be filled
     * @param fromIndex the index of the first element (inclusive)
     * @param toIndex   toIndex the index of the last element (exclusive)
     * @param value     value to be stored
     * @param <T>       type parameter
     */
    private static <T> void systemFill(T[] array, int fromIndex, int toIndex, T value) {
        int len;
        if ((len = toIndex - fromIndex) < 64) {
            fillObjects(array, fromIndex, toIndex, value);
        } else {
            array[fromIndex] = value;
            for (int i = 1; i < len; i += i) {
                System.arraycopy(array, fromIndex, array, i + fromIndex, Math.min((len - i), i));
            }
        }
    }

    /**
     * This method is an alternative to Arrays.fill() with the same
     * method signature
     *
     * @param array     the array to be filled
     * @param fromIndex the index of the first element (inclusive)
     * @param toIndex   toIndex the index of the last element (exclusive)
     * @param value     value to be stored
     */
    public static void systemFill(int[] array, int fromIndex, int toIndex, int value) {
        int len;
        if ((len = toIndex - fromIndex) < 64) {
            fillInts(array, fromIndex, toIndex, value);
        } else {
            array[fromIndex] = value;
            for (int i = 1; i < len; i += i) {
                System.arraycopy(array, fromIndex, array, i + fromIndex, Math.min((len - i), i));
            }
        }
    }

    /**
     * This method is an alternative to Arrays.fill() with the same
     * method signature
     *
     * @param array     the array to be filled
     * @param fromIndex the index of the first element (inclusive)
     * @param toIndex   toIndex the index of the last element (exclusive)
     * @param value     value to be stored
     */
    private static void systemFill(boolean[] array, int fromIndex, int toIndex, boolean value) {
        int len;
        if ((len = toIndex - fromIndex) < 64) {
            fillBooleans(array, fromIndex, toIndex, value);
        } else {
            array[fromIndex] = value;
            for (int i = 1; i < len; i += i) {
                System.arraycopy(array, fromIndex, array, i + fromIndex, Math.min((len - i), i));
            }
        }
    }

    private static void fillBooleans(boolean[] a, int fromIndex, int toIndex, boolean val) {
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    private static void fillInts(int[] a, int fromIndex, int toIndex, int val) {
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

    private static <T> void fillObjects(T[] a, int fromIndex, int toIndex, T val) {
        for (int i = fromIndex; i < toIndex; i++) a[i] = val;
    }

}
