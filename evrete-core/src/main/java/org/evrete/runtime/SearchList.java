package org.evrete.runtime;

import org.evrete.api.Named;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.annotations.Nullable;

import java.util.*;

public class SearchList<T extends Named> implements Iterable<T> {
    private final List<T> list = new ArrayList<>();
    private final Map<String, T> map = new HashMap<>();

    public void add(T t) {
        this.list.add(t);
        this.map.put(t.getName(), t);
    }

    public void sort(Comparator<? super T> comparator) {
        this.list.sort(comparator);
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    public List<T> getList() {
        return list;
    }

    @Nullable
    public T get(String name) {
        return map.get(name);
    }
}
