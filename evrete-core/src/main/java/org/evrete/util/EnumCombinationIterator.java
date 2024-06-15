package org.evrete.util;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Provides an iterator for all combinations of enum values for a given enum class <code>E</code>
 * inside a given array <code>E[]</code>. The total count is thus <code>pow(enum length, array size)</code>.
 * The iterator reuses the given array rather than creating a new one for each iteration, leading to
 * faster performance and reduced memory footprint.
 *
 * @param <E> the type of the enum
 */
public class EnumCombinationIterator<E extends Enum<E>> implements Iterator<E[]> {
    private final E[] enums;
    private final E[] state;
    private final long totalCombinations;
    private long currentCounter;

    /**
     *
     * @param enumClass enum class
     * @param state shared result array of combinations
     */
    public EnumCombinationIterator(Class<E> enumClass, E[] state) {
        this.enums = enumClass.getEnumConstants();
        this.state = state;
        this.totalCombinations = BigInteger.valueOf(enums.length).pow(state.length).longValue();
        this.currentCounter = 0;
        updateCurrentCombination();
    }

    @Override
    public boolean hasNext() {
        return currentCounter < totalCombinations;
    }

    private void updateCurrentCombination() {
        long counter = currentCounter;
        for (int i = state.length - 1; i >= 0; i--) {
            int index = (int) (counter % enums.length);
            state[i] = enums[index];
            counter /= enums.length;
        }
    }

    @Override
    public E[] next() {
        if(hasNext()) {
            currentCounter++;
            updateCurrentCombination();
            return state;
        } else {
            throw new NoSuchElementException();
        }
    }
}
