package org.evrete.runtime;

import org.evrete.api.Named;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

public class SearchList<T extends Named> implements Iterable<T> {
    private final List<T> list = new ArrayList<>();
    private final Map<String, T> map = new HashMap<>();

    public void add(T t) {
        synchronized (list) {
            this.list.add(t);
            this.map.put(t.getName(), t);
        }
    }

    public void addAllAndSort(Collection<T> collection, Comparator<? super T> comparator) {
        synchronized (list) {
            for (T t : collection) {
                this.list.add(t);
                this.map.put(t.getName(), t);
            }
            this.list.sort(comparator);
        }
    }

    public void sort(Comparator<? super T> comparator) {
        synchronized (list) {
            this.list.sort(comparator);
        }
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    public List<T> getList() {
        return list;
    }

    public Stream<T> stream() {
        return list.stream();
    }

    @Nullable
    public T get(String name) {
        return map.get(name);
    }
}
