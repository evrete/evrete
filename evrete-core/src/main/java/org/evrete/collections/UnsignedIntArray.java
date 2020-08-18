package org.evrete.collections;

import org.evrete.util.CollectionUtils;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

public class UnsignedIntArray {
    private static final int NULL_VALUE = -1;
    private int[] unsignedIndices;
    protected int currentInsertIndex;

    public UnsignedIntArray(int initialSize) {
        this.unsignedIndices = (int[]) Array.newInstance(int.class, Math.max(initialSize, 1));
        CollectionUtils.systemFill(this.unsignedIndices, NULL_VALUE);
        this.currentInsertIndex = 0;
    }

    private static int optimalArrayLen(int dataSize) {
        switch (dataSize) {
            case 0:
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 5;
            default:
                return (dataSize + 1) * 3 / 2;
        }
    }

    public void addNew(int value) {
        if (currentInsertIndex == unsignedIndices.length - 1) {
            expand();
        }
        unsignedIndices[currentInsertIndex++] = value;
    }

    protected int getAt(int pos) {
        return unsignedIndices[pos];
    }

    void clear() {
        this.currentInsertIndex = 0;
    }

    public int currentInsertIndex() {
        return currentInsertIndex;
    }

    int dataSize() {
        return unsignedIndices.length;
    }

    void copyFrom(UnsignedIntArray other) {
        this.unsignedIndices = other.unsignedIndices;
        this.currentInsertIndex = other.currentInsertIndex;
    }

    public void forEachInt(IntConsumer consumer) {
        int i, idx;
        for (i = 0; i < currentInsertIndex; i++) {
            idx = unsignedIndices[i];
            consumer.accept(idx);
        }
    }

    IntStream intStream() {
        return Arrays.stream(unsignedIndices, 0, currentInsertIndex);
    }

    public boolean delete(IntPredicate predicate) {
        if (currentInsertIndex == 0) return false;
        ArrayBulkCleanupData rs = new ArrayBulkCleanupData(currentInsertIndex);

        int deletedEntries = rs.clean(unsignedIndices, predicate);
        if (deletedEntries > 0) {
            CollectionUtils.systemFill(unsignedIndices, currentInsertIndex - deletedEntries, currentInsertIndex, -1);
            currentInsertIndex -= deletedEntries;
            shrink();
            return true;
        } else {
            return false;
        }
    }

    private void expand() {
        int newLen = optimalArrayLen(this.unsignedIndices.length);
        this.unsignedIndices = Arrays.copyOf(this.unsignedIndices, newLen);
        CollectionUtils.systemFill(unsignedIndices, currentInsertIndex, newLen, NULL_VALUE);
    }

    @Override
    public String toString() {
        return "{data=" + Arrays.toString(unsignedIndices) +
                ", currentIndex=" + currentInsertIndex +
                '}';
    }

    private void shrink() {
        int optimal = optimalArrayLen(currentInsertIndex);
        if (currentInsertIndex < optimal) {
            this.unsignedIndices = Arrays.copyOf(this.unsignedIndices, optimal);
        }
    }

}
