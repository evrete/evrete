package org.evrete.runtime;

import org.evrete.api.FieldsKey;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.memory.SessionMemory;
import org.evrete.runtime.structure.RuleDescriptor;
import org.evrete.util.CollectionUtils;

import java.util.*;

public class RuntimeRules implements Iterable<RuntimeRule>, MemoryChangeListener {
    private final List<RuntimeRule> list = new ArrayList<>();
    private final Collection<RuntimeAggregateLhsJoined> aggregateLhsGroups = new ArrayList<>();
    private final Map<FieldsKey, RuntimeFactType[][]> byFieldsAndAlphaBucket = new HashMap<>();
    private final SessionMemory runtime;

    public RuntimeRules(SessionMemory runtime) {
        this.runtime = runtime;
    }

    private void add(RuntimeRule rule) {
        this.list.add(rule);
        for (RuntimeFactType type : rule.getAllFactTypes()) {
            FieldsKey fields = type.getFields();
            if (fields.size() > 0) {
                byFieldsAndAlphaBucket.put(fields, rebuildTypes(fields, type));
            }
        }
        this.aggregateLhsGroups.addAll(rule.getAggregateLhsGroups());
    }

    private RuntimeFactType[][] rebuildTypes(FieldsKey fields, RuntimeFactType type) {
        int maxAlphaBucket = computeMaxAlphaBucket(fields);
        int alphaBucket = type.getBucketIndex();
        RuntimeFactType[][] types = byFieldsAndAlphaBucket.get(fields);
        RuntimeFactType[] bucketArr;
        if (types == null) {
            bucketArr = RuntimeFactType.ZERO_ARRAY;
            types = new RuntimeFactType[maxAlphaBucket + 1][];
        } else {
            if (maxAlphaBucket < types.length) {
                bucketArr = types[alphaBucket];
            } else {
                types = Arrays.copyOf(types, maxAlphaBucket + 1);
                bucketArr = RuntimeFactType.ZERO_ARRAY;
            }
        }

        RuntimeFactType[] updatedArray = CollectionUtils.appendToArray(bucketArr, type);
        types[alphaBucket] = updatedArray;
        CollectionUtils.fillIfNull(types, RuntimeFactType.ZERO_ARRAY);
        return types;
    }

    public RuntimeRule addRule(RuleDescriptor ruleDescriptor) {
        RuntimeRule r = new RuntimeRule(ruleDescriptor, runtime);
        this.add(r);
        return r;
    }

    public RuntimeFactType[][] getTypesByAlphaBucket(FieldsKey fields) {
        RuntimeFactType[][] arr = byFieldsAndAlphaBucket.get(fields);
        assert arr != null;
        return arr;
    }

    public Collection<RuntimeAggregateLhsJoined> getAggregateLhsGroups() {
        return aggregateLhsGroups;
    }

    @Override
    public Iterator<RuntimeRule> iterator() {
        return list.iterator();
    }

    public List<RuntimeRule> asList() {
        return list;
    }

    @Override
    public void onAfterChange() {
        for (RuntimeRule rule : list) {
            rule.onAfterChange();
        }
    }

    private int computeMaxAlphaBucket(FieldsKey fields) {
        ArrayOf<AlphaBucketMeta> maskCollection = runtime.getAlphaConditions().getAlphaMasks(fields, true);
        int ret = Integer.MIN_VALUE;

        for (AlphaBucketMeta mask : maskCollection.data) {
            ret = Math.max(ret, mask.getBucketIndex());
        }

        return ret;
    }

}
