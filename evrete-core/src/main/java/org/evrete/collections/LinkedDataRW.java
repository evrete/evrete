package org.evrete.collections;

import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;

import java.util.NoSuchElementException;
import java.util.StringJoiner;

public class LinkedDataRW<T> implements ReIterable<T> {
    private long size;
    private Node<T> first;
    private Node<T> last;

    public LinkedDataRW<T> add(T object) {
        Node<T> node;
        if (last == null) {
            // First entry
            node = new Node<>(object);
            this.first = node;
        } else {
            node = new Node<>(object);
            this.last.next = node;
        }
        this.last = node;
        this.size++;
        return this;
    }

    //TODO !!! the usage can be optimized
    public void append(LinkedDataRW<T> other) {
        other.iterator().forEachRemaining(LinkedDataRW.this::add);
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(",", "[", "]");
        iterator().forEachRemaining(t -> sj.add(t == null ? "null" : t.toString()));
        return sj.toString();
    }

    public long size() {
        return size;
    }

    public void clear() {
        this.first = null;
        this.last = null;
        this.size = 0;
    }

    @Override
    public ReIterator<T> iterator() {
        return new It();
    }

    static class Node<Z> {
        Node<Z> next;
        Z data;

        Node(Z data) {
            this.data = data;
        }
    }

    private class It implements ReIterator<T> {
        Node<T> next;

        It() {
            this.next = first;
        }

        @Override
        public long reset() {
            this.next = first;
            return size;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public T next() {
            if (next == null) {
                throw new NoSuchElementException();
            } else {
                T o = next.data;
                this.next = this.next.next;
                return o;
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
