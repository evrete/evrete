package pkg1.evrete.tests.classes;

import java.io.File;

public class IntValue {
    public final int value;

    public IntValue(int value) {
        this.value = value;
    }

    public boolean testFile(Object f) {
        return new File(f.toString()).exists();
    }
}