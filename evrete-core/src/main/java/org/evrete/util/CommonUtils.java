package org.evrete.util;

import org.evrete.api.LhsField;
import org.evrete.api.NamedType;
import org.evrete.api.Type;
import org.evrete.api.TypeField;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public final class CommonUtils {
    private static final CompletableFuture<?>[] EMPTY_FUTURES = new CompletableFuture[0];


    @SuppressWarnings("unchecked")
    public static <T> T[] array(Class<T> type, int size) {
        return (T[]) Array.newInstance(type, size);
    }

    public static <T> Set<T> newIdentityHashSet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    public static <T> Set<T> newIdentityHashSet(Collection<T> collection) {
        Set<T> set = newIdentityHashSet();
        set.addAll(collection);
        return set;
    }

    public static <K, V> V[] mapArray(Class<V> type, K[] arr, Function<K, V> mapper) {
        V[] result = array(type, arr.length);
        for (int i = 0; i < result.length; i++) {
            result[i] = mapper.apply(arr[i]);
        }
        return result;
    }

    public static int[] toPrimitives(Collection<Integer> collection) {
        int[] result = new int[collection.size()];
        int i = 0;
        for (Integer integer : collection) {
            result[i++] = integer;
        }
        return result;
    }


    public static <T> T[] copyOf(T[] arr) {
        return Arrays.copyOf(arr, arr.length);
    }

    public static <E> List<List<E>> permutation(List<E> l) {
        ArrayList<E> original = new ArrayList<>(l); // ArrayList supports remove
        if (original.isEmpty()) {
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
     * @param <E> source type parameter
     * @param <T> collection type parameter
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

    private static <K, V, C extends Collection<V>> List<Map<K, V>> combinations(Map<K, C> sources, Supplier<Map<K, V>> emptyMapSupplier) {
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

    public static Optional<Collection<?>> resolveCollection(Object o) {

        if (o.getClass().isArray()) {
            return Optional.of(Arrays.asList((Object[]) o));
        } else if (o instanceof Iterable) {
            Collection<Object> ret = new LinkedList<>();
            ((Iterable<?>) o).forEach((Consumer<Object>) ret::add);
            return Optional.of(ret);
        } else {
            return Optional.empty();
        }
    }

    public static Collection<?> toCollection(Object o) {
        return resolveCollection(o).orElse(Collections.singleton(o));
    }

    public static void systemFill(int[] array, int value) {
        systemFill(array, array.length, value);
    }

    public static void systemFill(boolean[] array, boolean value) {
        systemFill(array, array.length, value);
    }

    public static <T> void systemFill(T[] array, T value) {
        systemFill(array, array.length, value);
    }

    /**
     * This method is an alternative to Arrays.fill() with the same
     * method signature
     *
     * @param <T>     type parameter
     * @param array   the array to be filled
     * @param toIndex toIndex the index of the last element (exclusive)
     * @param value   value to be stored
     */
    private static <T> void systemFill(T[] array, int toIndex, T value) {
        int len;
        if ((len = toIndex) < 64) {
            fillObjects(array, toIndex, value);
        } else {
            array[0] = value;
            for (int i = 1; i < len; i += i) {
                System.arraycopy(array, 0, array, i, Math.min((len - i), i));
            }
        }
    }

    /**
     * This method is an alternative to Arrays.fill() with the same
     * method signature
     *
     * @param array   the array to be filled
     * @param toIndex toIndex the index of the last element (exclusive)
     * @param value   value to be stored
     */
    private static void systemFill(int[] array, int toIndex, int value) {
        int len;
        if ((len = toIndex) < 64) {
            fillIntegers(array, toIndex, value);
        } else {
            array[0] = value;
            for (int i = 1; i < len; i += i) {
                System.arraycopy(array, 0, array, i, Math.min((len - i), i));
            }
        }
    }

    /**
     * This method is an alternative to Arrays.fill() with the same
     * method signature
     *
     * @param array   the array to be filled
     * @param toIndex toIndex the index of the last element (exclusive)
     * @param value   value to be stored
     */
    private static void systemFill(boolean[] array, int toIndex, boolean value) {
        int len;
        if ((len = toIndex) < 64) {
            fillBooleans(array, toIndex, value);
        } else {
            array[0] = value;
            for (int i = 1; i < len; i += i) {
                System.arraycopy(array, 0, array, i, Math.min((len - i), i));
            }
        }
    }

    private static void fillBooleans(boolean[] a, int toIndex, boolean val) {
        for (int i = 0; i < toIndex; i++) a[i] = val;
    }

    private static void fillIntegers(int[] a, int toIndex, int val) {
        for (int i = 0; i < toIndex; i++) a[i] = val;
    }

    private static <T> void fillObjects(T[] a, int toIndex, T val) {
        for (int i = 0; i < toIndex; i++) a[i] = val;
    }

    public static byte[] toByteArray(InputStream is) {
        try {
            return toByteArrayChecked(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    public static String toString(Reader reader) throws IOException {
        char[] arr = new char[8192];
        StringBuilder buffer = new StringBuilder();
        int numRead;
        while ((numRead = reader.read(arr, 0, arr.length)) != -1) {
            buffer.append(arr, 0, numRead);
        }
        return buffer.toString();
    }

    public static byte[] toByteArrayChecked(InputStream is) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            int nRead;
            byte[] data = new byte[8192];

            while ((nRead = is.read(data, 0, data.length)) != -1) {
                bos.write(data, 0, nRead);
            }

            bos.flush();
            return bos.toByteArray();
        }
    }

    public static <A, R, S extends Closeable> R[] read(Class<R> type, A[] args, IOFunction<A, S> reader, IOFunction<S, R> mapper) throws IOException {
        int length = args.length;
        R[] result = array(type, length);

        for (int i = 0; i < length; i++) {
            try (S stream = reader.apply(args[i])) {
                result[i] = mapper.apply(stream);
            }
        }
        return result;
    }


    public static byte[] bytes(JarFile jarFile, ZipEntry entry) {
        try {
            return toByteArray(jarFile.getInputStream(entry));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <V> CompletableFuture<List<V>> completeAndCollect(List<CompletableFuture<V>> futures) {
        if(futures.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        } else {
            CompletableFuture<Void> completedFactInserts = CompletableFuture.allOf(futures.toArray(EMPTY_FUTURES));
            return completedFactInserts.thenApply(unused -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
        }
    }

    public static <V, T> CompletableFuture<List<V>> completeAndCollect(Collection<T> list, Function<T, CompletableFuture<V>> mapper) {
        if(list.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        } else {
            List<CompletableFuture<V>> futureList = new ArrayList<>(list.size());
            for(T v : list) {
                futureList.add(mapper.apply(v));
            }
            return completeAndCollect(futureList);
        }
    }

    public static <V, T> CompletableFuture<List<V>> completeAndCollect(Iterable<T> list, Function<T, CompletableFuture<V>> mapper) {
        List<CompletableFuture<V>> futureList = new LinkedList<>();
        for (T obj : list) {
            futureList.add(mapper.apply(obj));
        }
        return completeAndCollect(futureList);
    }

    public static <T> CompletableFuture<Void> completeAll(Collection<CompletableFuture<T>> futures) {
        if(futures.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        } else {
            return CompletableFuture.allOf(futures.toArray(EMPTY_FUTURES));
        }
    }

    public static <T, F> CompletableFuture<Void> completeAll(T[] arr, Function<T, CompletableFuture<F>> futureFunction) {
        return completeAll(Arrays.asList(arr), futureFunction);
    }

    public static <T, F> CompletableFuture<Void> completeAll(Collection<T> collection, Function<T, CompletableFuture<F>> futureFunction) {
        if(collection.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        } else {
            List<CompletableFuture<F>> futuresList = new ArrayList<>();
            for (T t : collection) {
                futuresList.add(futureFunction.apply(t));
            }
            return completeAll(futuresList);
        }
    }

    public static LhsField<String, TypeField> toTypeField(LhsField<String, String> lhsField, NamedType.Resolver namedTypeResolver) {
        String factName = lhsField.fact();
        Type<?> type = namedTypeResolver.resolve(factName).getType();
        String rawFieldName = lhsField.field();
        if(rawFieldName == null) {
            rawFieldName = "";
        }
        TypeField typeField = type.getField(rawFieldName);
        return new LhsField<>(factName, typeField);
    }
}
