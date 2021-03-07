package org.evrete.collections;

import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;

import java.util.NoSuchElementException;
import java.util.StringJoiner;

public class LinkedDataRWD<T> implements ReIterable<T> {
    private long size;
    private Node<T> first;
    private Node<T> last;

    public LinkedDataRWD<T> add(T object) {
        Node<T> node;
        if (last == null) {
            // First entry
            node = new Node<>(object, null);
            this.first = node;
        } else {
            node = new Node<>(object, last);
            this.last.next = node;
            node.prev = this.last;
        }
        this.last = node;
        updateSize(1);
        return this;
    }

    private void updateSize(long delta) {
        this.size += delta;
    }


    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(",", "[", "]");
        iterator().forEachRemaining(t -> sj.add(t == null ? "null" : t.toString()));
        return sj.toString();
    }

    /**
     * <p>
     * This method moves other data's to the end of this collection. The argument gets emptied upon completion.
     * </p>
     *
     * @param other target data to consume and clear
     */
    public void consume(LinkedDataRWD<T> other) {
        if (other.last == null) {
            assert other.first == null && other.size == 0;
            return; // Nothing to append
        }
        if (this.last == null) {
            // This collection is empty
            this.first = other.first();
            this.last = other.last();
            this.size = other.size;
        } else {
            this.last.next = other.first;
            other.first.prev = this.last;
            this.last = other.last;
        }
        updateSize(other.size);
        other.clear();
    }

    public long size() {
        return size;
    }

    Node<T> first() {
        return first;
    }

    Node<T> last() {
        return last;
    }

    private void setFirst(Node<T> node) {
        this.first = node;
        if (node == null) {
            this.last = null;
            this.size = 0;
        } else {
            this.first.prev = null;
        }
    }

    private void setLast(Node<T> node) {
        this.last = node;
        if (node == null) {
            this.first = null;
            this.size = 0;
        } else {
            this.last.next = null;
        }
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
        Node<Z> prev;
        Node<Z> next;
        Z data;

        Node(Z data, Node<Z> prev) {
            this.data = data;
            this.prev = prev;
        }

        void drop() {
            this.next.prev = this.prev;
            this.prev.next = this.next;
            clearRefs();
        }

        void clearRefs() {
            this.next = null;
            this.prev = null;
        }
    }


    private class It implements ReIterator<T> {
        Node<T> next;
        Node<T> last;

        It() {
            this.next = first;
        }

        @Override
        public long reset() {
            this.next = first;
            this.last = null;
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
                last = next;
                next = last.next;
                return last.data;
            }
        }

        @Override
        public void remove() {
            if (last == null) throw new IllegalStateException("Iterator: remove() without next()");
            // TODO move this to the enclosing class
            updateSize(-1);
            if (last.prev == null) {
                // Last was the first node
                setFirst(last.next);
                last.clearRefs();
            } else if (last.next == null) {
                // Last was the last node
                setLast(last.prev);
                last.clearRefs();
            } else {
                last.drop();
            }
        }
    }
}
