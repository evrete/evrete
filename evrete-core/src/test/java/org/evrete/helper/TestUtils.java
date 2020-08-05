package org.evrete.helper;

import org.evrete.api.ReIterator;
import org.evrete.api.StatefulSession;
import org.evrete.collections.CollectionReIterator;
import org.evrete.collections.FastHashMap;
import org.evrete.collections.FastHashSet;
import org.evrete.util.CollectionUtils;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public final class TestUtils {

    public static long nanoExecTime(Runnable r) {
        long t0 = System.nanoTime();
        r.run();
        return System.nanoTime() - t0;
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Collection<Object> sessionObjects(StatefulSession s) {
        Collection<Object> col = new LinkedList<>();
        s.forEachMemoryObject(col::add);
        return col;
    }

    public static <T> Collection<T> sessionObjects(StatefulSession s, Class<T> type) {
        Collection<T> col = new LinkedList<>();
        s.forEachMemoryObject(type, col::add);
        return col;
    }

    public static KieContainer droolsKnowledge(String file) {
        KieServices ks = KieServices.get();
        //ks.newKieBaseConfiguration()
        KieRepository kr = ks.getRepository();
        KieFileSystem kfs = ks.newKieFileSystem();

        kfs.write(ResourceFactory.newFileResource(new File(file)));

        KieBuilder kb = ks.newKieBuilder(kfs);

        kb.buildAll(); // kieModule is automatically deployed to KieRepository if successfully built.
        if (kb.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Build Errors:\n" + kb.getResults().toString());
        }

        return ks.newKieContainer(kr.getDefaultReleaseId());
    }

    public static <Z> IterableSet<Z> setOf(Set<Z> set) {
        return new IterableSet<Z>() {
            @Override
            public boolean contains(Z element) {
                return set.contains(element);
            }

            @Override
            public ReIterator<Z> iterator() {
                return new CollectionReIterator<>(set);
            }

            @Override
            public boolean remove(Z element) {
                return set.remove(element);
            }

            @Override
            public boolean add(Z element) {
                return set.add(element);
            }

            @Override
            public long size() {
                return set.size();
            }

            @Override
            public Stream<Z> stream() {
                return set.stream();
            }

            @Override
            public void delete(Predicate<Z> predicate) {
                CollectionUtils.deleteFrom(set, predicate);
            }

            @Override
            public void clear() {
                set.clear();
            }

            @Override
            public void forEach(Consumer<Z> consumer) {
                set.forEach(consumer);
            }

            @Override
            public String toString() {
                return set.toString();
            }
        };
    }

    public static <Z> IterableSet<Z> setOf(FastHashSet<Z> set) {
        return new IterableSet<Z>() {
            @Override
            public boolean contains(Z element) {
                return set.contains(element);
            }

            @Override
            public ReIterator<Z> iterator() {
                return set.iterator();
            }

            @Override
            public boolean remove(Z element) {
                return set.remove(element);
            }

            @Override
            public boolean add(Z element) {
                return set.add(element);
            }

            @Override
            public long size() {
                return set.size();
            }

            @Override
            public Stream<Z> stream() {
                return set.stream();
            }

            @Override
            public void delete(Predicate<Z> predicate) {
                set.delete(predicate);
            }

            @Override
            public void clear() {
                set.clear();
            }

            @Override
            public void forEach(Consumer<Z> consumer) {
                set.forEach(consumer);
            }

            @Override
            public String toString() {
                return set.toString();
            }
        };
    }

    public static <K1, V1> Mapping<K1, V1> map(Map<K1, V1> map) {
        return new Mapping<K1, V1>() {
            @Override
            public V1 put(K1 key, V1 value) {
                return map.put(key, value);
            }

            @Override
            public void forEachEntry(BiConsumer<K1, V1> consumer) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void forEachValue(Consumer<V1> consumer) {
                map.values().forEach(consumer);
            }

            @Override
            public void forEachKey(Consumer<K1> consumer) {
                map.keySet().forEach(consumer);
            }

            @Override
            public V1 computeIfAbsent(K1 key, Function<K1, V1> function) {
                return map.computeIfAbsent(key, function);
            }

            @Override
            public long size() {
                return map.size();
            }

            @Override
            public V1 get(K1 key) {
                return map.get(key);
            }

            @Override
            public void clear() {
                map.clear();
            }

            @Override
            public V1 remove(K1 key) {
                return map.remove(key);
            }

        };
    }

    public static <K1, V1> Mapping<K1, V1> map(FastHashMap<K1, V1> map) {
        return new Mapping<K1, V1>() {
            @Override
            public V1 put(K1 key, V1 value) {
                return map.put(key, value);
            }

            @Override
            public void forEachEntry(BiConsumer<K1, V1> consumer) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void forEachValue(Consumer<V1> consumer) {
                map.forEachValue(consumer);
            }

            @Override
            public void forEachKey(Consumer<K1> consumer) {
                map.forEachKey(consumer);
            }

            @Override
            public V1 computeIfAbsent(K1 key, Function<K1, V1> function) {
                return map.computeIfAbsent(key, function);
            }

            @Override
            public long size() {
                return map.size();
            }

            @Override
            public V1 get(K1 key) {
                return map.get(key);
            }

            @Override
            public void clear() {
                map.clear();
            }

            @Override
            public V1 remove(K1 key) {
                return map.remove(key);
            }

        };
    }

}
