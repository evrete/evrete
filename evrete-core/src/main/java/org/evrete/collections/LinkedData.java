package org.evrete.collections;

import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;

import java.util.NoSuchElementException;

public class LinkedData<T> implements ReIterable<T> {
    private long size;
    private Node<T> first;
    private Node<T> last;

    public LinkedData<T> add(T object) {
        Node<T> node;
        if (last == null) {
            //TODO !!! remove assertions
            assert first == null;
            assert size == 0;
            // First entry
            node = new Node<>(object, null);
            this.first = node;
            this.last = node;
        } else {
            node = new Node<>(object, last);
            last.next = node;
            node.prev = last;
        }
        this.last = node;
        this.size++;
        return this;
    }

    /**
     * <p>
     * This method moves other data's to the end of this collection. The argument gets emptied upon completion.
     * </p>
     *
     * @param other target data to consume and clear
     */
    public void consume(LinkedData<T> other) {
        if (other.last == null) {
            assert other.first == null && other.size == 0;
            return; // Nothing to append
        }
        if (this.last == null) {
            // This collection is empty
            assert this.first == null;
            assert this.size == 0 : "Actual: " + this.size;
            this.first = other.first();
            this.last = other.last();
            this.size = other.size;
        } else {
            this.last.next = other.first;
            other.first.prev = this.last;
            this.last = other.last;
        }
        this.size += other.size;
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
            LinkedData.this.size--;
            if (last.prev == null) {
                // Last was the first node
                LinkedData.this.setFirst(last.next);
                last.clearRefs();
            } else if (last.next == null) {
                // Last was the last node
                LinkedData.this.setLast(last.prev);
                last.clearRefs();
            } else {
                last.drop();
            }
        }
    }
}
