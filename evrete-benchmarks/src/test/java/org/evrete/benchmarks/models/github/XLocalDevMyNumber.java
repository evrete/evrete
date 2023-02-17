package org.evrete.benchmarks.models.github;

/**
 * <p>
 * Bug testing, <a href="https://github.com/andbi/evrete/issues/11">Bug with activation mode</a>
 * </p>
 */
public class XLocalDevMyNumber {
    private int number;

    public XLocalDevMyNumber(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }
}
