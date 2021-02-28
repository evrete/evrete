package org.evrete.helper;

import org.evrete.api.FactHandle;
import org.evrete.api.ReIterator;
import org.evrete.api.StatefulSession;
import org.evrete.collections.CollectionReIterator;
import org.evrete.collections.LinearHashSet;
import org.evrete.util.CollectionUtils;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.utils.KieHelper;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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

    public static Collection<FactEntry> sessionFacts(StatefulSession s) {
        Collection<FactEntry> col = new LinkedList<>();
        s.forEachFact(new BiConsumer<FactHandle, Object>() {
            @Override
            public void accept(FactHandle handle, Object fact) {
                col.add(new FactEntry(handle, fact));
            }
        });
        return col;
    }

    public static <T> Collection<FactEntry> sessionFacts(StatefulSession s, Class<T> type) {
        Collection<FactEntry> collection = new LinkedList<>();
        s.forEachFact((handle, fact) -> {
            if (fact.getClass().equals(type)) {
                collection.add(new FactEntry(handle, fact));
            }
        });
        return collection;
    }

    public static KieContainer droolsKnowledge(String file) {
        KieServices ks = KieServices.get();
        KieRepository kr = ks.getRepository();
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.write(ResourceFactory.newFileResource(new File(file)));
        KieBuilder kb = ks.newKieBuilder(kfs);
        kb.buildAll();
        if (kb.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Build Errors:\n" + kb.getResults().toString());
        }

        return ks.newKieContainer(kr.getDefaultReleaseId());
    }

    public static KieBase droolsKnowledge1(String file) {
        KieHelper helper = new KieHelper();
        Resource resource = ResourceFactory.newFileResource(file);
        helper.addResource(resource, ResourceType.DRL);
        Results results = helper.verify();
        if (results.hasMessages(Message.Level.ERROR)) {
            System.out.println(results.getMessages());
            throw new RuntimeException("Build Errors:\n" + results.toString());
        }
        return helper.build();
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

    public static <Z> IterableSet<Z> setOf(LinearHashSet<Z> set) {
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
                return set.addVerbose(element);
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
}
