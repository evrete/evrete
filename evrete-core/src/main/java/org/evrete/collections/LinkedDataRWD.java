package org.evrete.collections;

import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;

import java.util.NoSuchElementException;
import java.util.StringJoiner;

public class LinkedDataRWD<T> implements ReIterable<T> {
    private long size;
    private Node<T> firstNode;
    private Node<T> lastNode;

    public LinkedDataRWD<T> add(T object) {
        Node<T> node;
        if (lastNode == null) {
            // First entry
            node = new Node<>(object, null);
            this.firstNode = node;
        } else {
            node = new Node<>(object, lastNode);
            this.lastNode.next = node;
            node.prev = this.lastNode;
        }
        this.lastNode = node;
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
        if (other.lastNode == null) {
            assert other.firstNode == null && other.size == 0;
            return; // Nothing to append
        }
        if (this.lastNode == null) {
            // This collection is empty
            this.firstNode = other.first();
            this.lastNode = other.last();
            this.size = other.size;
        } else {
            this.lastNode.next = other.firstNode;
            other.firstNode.prev = this.lastNode;
            this.lastNode = other.lastNode;
        }
        updateSize(other.size);
        other.clear();
    }

    public long size() {
        return size;
    }

    Node<T> first() {
        return firstNode;
    }

    Node<T> last() {
        return lastNode;
    }

    private void setFirst(Node<T> node) {
        this.firstNode = node;
        if (node == null) {
            this.lastNode = null;
            this.size = 0;
        } else {
            this.firstNode.prev = null;
        }
    }

    private void setLast(Node<T> node) {
        this.lastNode = node;
        if (node == null) {
            this.firstNode = null;
            this.size = 0;
        } else {
            this.lastNode.next = null;
        }
    }

    public void clear() {
        this.firstNode = null;
        this.lastNode = null;
        this.size = 0;
    }

    private void removeNode(Node<T> last) {
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

    @Override
    public ReIterator<T> iterator() {
        return new It();
    }

    static class Node<Z> {
        Node<Z> prev;
        Node<Z> next;
        final Z data;

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
            this.next = firstNode;
        }

        @Override
        public long reset() {
            this.next = firstNode;
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
                this.last = this.next;
                this.next = this.last.next;
                return this.last.data;
            }
        }

        @Override
        public void remove() {
            if (last == null) throw new IllegalStateException("Iterator: remove() without next()");
            removeNode(last);
        }
    }
}
