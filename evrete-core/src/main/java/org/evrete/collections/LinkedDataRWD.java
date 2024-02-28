package org.evrete.collections;

import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;
import org.evrete.api.annotations.NonNull;

import java.util.NoSuchElementException;
import java.util.StringJoiner;

/**
 * Represents a linked list data structure that allows reading, writing, and delete operations.
 *
 * @param <T> the type of elements in the list
 */
public class LinkedDataRWD<T> implements ReIterable<T> {
    private long size;
    private Node<T> firstNode;
    private Node<T> lastNode;

    public LinkedDataRWD<T> add(T object) {
        final Node<T> newLastNode;
        if (lastNode == null) {
            // First entry
            newLastNode = new Node<>(object, null);
            this.firstNode = newLastNode;
        } else {
            Node<T> oldLastNode = this.lastNode;

            newLastNode = new Node<>(object, oldLastNode);
            oldLastNode.next = newLastNode;
            newLastNode.prev = oldLastNode;
        }
        this.lastNode = newLastNode;
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
        if (other.lastNode != null) {
            if (this.lastNode == null) {
                // Copy data
                this.lastNode = other.lastNode;
                this.firstNode = other.firstNode;
                this.size = other.size;
            } else {
                Node<T> myOldLastNode = this.lastNode;
                // Re-assign last node
                this.lastNode = other.lastNode;
                // Join nodes
                myOldLastNode.next = other.firstNode;
                other.firstNode.prev = myOldLastNode;
                // Update size
                this.size += other.size;
            }

            // Finally, clear the source
            other.clear();
        }
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

    private void removeNode(Node<T> node) {
        updateSize(-1);
        if (node.prev == null) {
            // Last was the first node
            setFirst(node.next);
            node.clearRefs();
        } else if (node.next == null) {
            // Last was the last node
            setLast(node.prev);
            node.clearRefs();
        } else {
            node.drop();
        }
    }

    @NonNull
    @Override
    public ReIterator<T> iterator() {
        return new It();
    }

    static class Node<Z> {
        final Z data;
        Node<Z> prev;
        Node<Z> next;

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
