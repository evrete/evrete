package org.evrete.collections;

public class LinearHashTestBase {


    static void assertData(AbstractLinearHash<?> data) {
        int actualDeleted = 0;
        for(AbstractLinearHash.Entry entry : data.data) {
            if(entry != null && entry.deleted) {
                actualDeleted++;
            }
        }

        assert actualDeleted == data.deletes: "Actual: " + actualDeleted + " vs " + data.deletes;
    }
}
