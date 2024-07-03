package org.evrete.util;

import org.evrete.api.LhsField;
import org.evrete.api.NamedType;
import org.evrete.api.Type;
import org.evrete.api.TypeField;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public final class CommonUtils {
    private static final CompletableFuture<?>[] EMPTY_FUTURES = new CompletableFuture[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];


    @SuppressWarnings("unchecked")
    public static <T> T[] array(Class<T> type, int size) {
        return (T[]) Array.newInstance(type, size);
    }

    public static <T> Set<T> newIdentityHashSet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }


    public static <K, V> V[] mapArray(Class<V> type, K[] arr, Function<K, V> mapper) {
        V[] result = array(type, arr.length);
        for (int i = 0; i < result.length; i++) {
            result[i] = mapper.apply(arr[i]);
        }
        return result;
    }

    @NonNull
    public static String[] splitConfigString(@Nullable String arg) {
        return splitConfigString(arg, "[\\s+,;]");
    }
    @NonNull
    public static String[] splitCSV(@Nullable String arg) {
        return splitConfigString(arg, "[,]");
    }

    @NonNull
    private static String[] splitConfigString(@Nullable String arg, String pattern) {
        if(arg == null || arg.isEmpty()) {
            return EMPTY_STRING_ARRAY;
        } else {
            String[] parts = arg.trim().split(pattern);
            List<String> result = new ArrayList<>(parts.length);
            for(String part : parts) {
                String trimmed = part.trim();
                if(!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
            return result.toArray(EMPTY_STRING_ARRAY);
        }
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
