package org.evrete.collections;

import org.evrete.util.CollectionUtils;

import java.util.function.IntPredicate;

class ArrayBulkCleanupData {
    private static final int NULL_VALUE = -1;
    private final int[] indices;
    private final int[] lengths;
    private int currentIndex;

    public ArrayBulkCleanupData(int size) {
        indices = new int[size];
        lengths = new int[size];
        CollectionUtils.systemFill(indices, NULL_VALUE);
        CollectionUtils.systemFill(lengths, NULL_VALUE);
        this.currentIndex = NULL_VALUE;
    }

    private void add(int i) {
        if (currentIndex == NULL_VALUE) {
            // First run
            currentIndex = 0;
            indices[currentIndex] = i;
            lengths[currentIndex] = 1;
        } else {
            if (indices[currentIndex] + lengths[currentIndex] == i) {
                lengths[currentIndex]++;
            } else {
                currentIndex++;
                indices[currentIndex] = i;
                lengths[currentIndex] = 1;
            }
        }
    }

    int clean(int[] data, IntPredicate predicate) {
        for (int i = 0; i < indices.length; i++) {
            if (predicate.test(data[i])) {
                this.add(i);
            }
        }
        return applyTo(data, indices.length);
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    private int applyTo(Object array, int initialSize) {
        int totalDeleted = 0;
        for (int i = 0; i < indices.length; i++) {
            int idx = indices[i];
            if (idx == NULL_VALUE) break;
            int adjustedIdx = idx - totalDeleted;
            int len = lengths[i];

            int srcPos = idx + len - totalDeleted;
            int copyLength = initialSize - idx - len;
            System.arraycopy(array, srcPos, array, adjustedIdx, copyLength);
            totalDeleted += len;
        }
        return totalDeleted;
    }
}
