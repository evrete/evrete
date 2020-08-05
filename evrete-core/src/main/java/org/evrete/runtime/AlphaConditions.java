package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.collections.ArrayOf;

import java.util.*;

import static org.evrete.api.LogicallyComparable.*;

public class AlphaConditions implements Copyable<AlphaConditions> {
    private static final ArrayOf<AlphaEvaluator> EMPTY = new ArrayOf<>(new AlphaEvaluator[0]);
    private static final ArrayOf<AlphaMask> EMPTY_MASKS = new ArrayOf<>(new AlphaMask[0]);
    private final Map<Type, ArrayOf<AlphaEvaluator>> alphaPredicates;
    private final Map<Type, TypeAlphas> typeAlphas;

    private AlphaConditions(AlphaConditions other) {
        this.alphaPredicates = new HashMap<>();
        for (Map.Entry<Type, ArrayOf<AlphaEvaluator>> entry : other.alphaPredicates.entrySet()) {
            this.alphaPredicates.put(entry.getKey(), new ArrayOf<>(entry.getValue()));
        }

        this.typeAlphas = new HashMap<>();
        other.typeAlphas.forEach((fields, alphas) -> AlphaConditions.this.typeAlphas.put(fields, alphas.copyOf()));
    }

    public AlphaConditions() {
        this.typeAlphas = new HashMap<>();
        this.alphaPredicates = new HashMap<>();
    }

    @Override
    public AlphaConditions copyOf() {
        return new AlphaConditions(this);
    }

    public int size(Type type) {
        return alphaPredicates.getOrDefault(type, EMPTY).data.length;
    }

    public synchronized AlphaMask register(AbstractRuntime<?, ?> runtime, FieldsKey betaFields, boolean beta, Set<Evaluator> typePredicates) {
        if (typePredicates.isEmpty() && betaFields.size() == 0) {
            return AlphaMask.NO_FIELDS_NO_CONDITIONS;
        }

        Type type = betaFields.getType();
        AlphaMeta candidate = createAlphaMask(runtime, type, typePredicates);
        return typeAlphas.computeIfAbsent(type, TypeAlphas::new).getCreate(betaFields, beta, candidate);
    }

    public ArrayOf<AlphaMask> getAlphaMasks(FieldsKey fields, boolean beta) {
        Type type = fields.getType();
        TypeAlphas typeData = typeAlphas.get(type);
        if (typeData == null) {
            return EMPTY_MASKS;
        } else {
            return typeData.getAlphaMasks(fields, beta);
        }
    }

    public int computeMaxAlphaBucket(FieldsKey fields, boolean beta) {
        ArrayOf<AlphaMask> maskCollection = getAlphaMasks(fields, beta);
        int ret = Integer.MIN_VALUE;

        for (AlphaMask mask : maskCollection.data) {
            ret = Math.max(ret, mask.getBucketIndex());
        }

        return ret;
    }

    public ArrayOf<AlphaEvaluator> getPredicates(Type t) {
        return alphaPredicates.getOrDefault(t, EMPTY);
    }

    @Override
    public String toString() {
        return "AlphaConditions{" +
                "alphaPredicates=" + alphaPredicates +
                '}';
    }


    private AlphaMeta createAlphaMask(AbstractRuntime<?, ?> runtime, Type t, Set<Evaluator> typePredicates) {
        ArrayOf<AlphaEvaluator> existing = alphaPredicates.computeIfAbsent(t, k -> new ArrayOf<>(new AlphaEvaluator[0]));

        List<EvaluationSide> mapping = new ArrayList<>(typePredicates.size());

        for (Evaluator alphaPredicate : typePredicates) {
            AlphaEvaluator found = null;
            boolean foundDirect = true;

            for (AlphaEvaluator ia : existing.data) {
                int cmp = alphaPredicate.compare(ia.getDelegate());
                switch (cmp) {
                    case RELATION_EQUALS:
                        found = ia;
                        foundDirect = true;
                        break;
                    case RELATION_INVERSE:
                        found = ia;
                        foundDirect = false;
                        break;
                    case RELATION_NONE:
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }

            if (found == null) {
                TypeField field = alphaPredicate.descriptor()[0].field();
                found = new AlphaEvaluator(existing.data.length, alphaPredicate, runtime.getCreateActiveField(field));
                existing.append(found);
            }

            mapping.add(new EvaluationSide(found, foundDirect));
        }

        boolean[] validValues = new boolean[existing.data.length];
        int[] mappedIndices = new int[mapping.size()];

        int mappingIdx = 0;
        for (EvaluationSide h : mapping) {
            int alphaId = h.condition.getIndex();
            mappedIndices[mappingIdx++] = alphaId;
            validValues[alphaId] = h.direct;
        }

        Arrays.sort(mappedIndices); // Required for further compare operations
        return new AlphaMeta(mappedIndices, validValues);
    }

    private static class EvaluationSide {
        private final AlphaEvaluator condition;
        private final boolean direct;

        EvaluationSide(AlphaEvaluator condition, boolean direct) {
            this.condition = condition;
            this.direct = direct;
        }
    }

    private static class FieldAlphas implements Copyable<FieldAlphas> {
        private final Set<AlphaMask> dataOld;
        private final ArrayOf<AlphaMask> data;

        FieldAlphas(FieldsKey fields) {
            this.dataOld = new HashSet<>();
            this.data = new ArrayOf<>(new AlphaMask[0]);
        }

        FieldAlphas(FieldAlphas other) {
            this.dataOld = new HashSet<>(other.dataOld);
            this.data = new ArrayOf<>(other.data);
        }

        AlphaMask getCreate(AlphaMeta candidate) {
            AlphaMask found = null;
            for (AlphaMask mask : data.data) {
                if (mask.sameData(candidate.alphaIndices, candidate.requiredValues)) {
                    found = mask;
                    break;
                }
            }
            if (found == null) {
                found = AlphaMask.factory(data.data.length, candidate.alphaIndices, candidate.requiredValues);
                data.append(found);
            }

            return found;
        }

        @Override
        public FieldAlphas copyOf() {
            return new FieldAlphas(this);
        }
    }

    private static class TypeAlphas implements Copyable<TypeAlphas> {
        private final Map<FieldsKey, FieldAlphas> dataAlpha;
        private final Map<FieldsKey, FieldAlphas> dataBeta;

        TypeAlphas(Type type) {
            this.dataAlpha = new HashMap<>();
            this.dataBeta = new HashMap<>();
        }

        TypeAlphas(TypeAlphas other) {
            this.dataAlpha = new HashMap<>();
            this.dataBeta = new HashMap<>();
            this.dataAlpha.putAll(other.dataAlpha);
            this.dataBeta.putAll(other.dataBeta);
        }

        public ArrayOf<AlphaMask> getAlphaMasks(FieldsKey fields, boolean beta) {
            Map<FieldsKey, FieldAlphas> map = beta ?
                    dataBeta
                    :
                    dataAlpha;
            FieldAlphas fieldData = map.get(fields);
            if (fieldData == null) {
                return EMPTY_MASKS;
            } else {
                return fieldData.data;
            }
        }

        private AlphaMask getCreate(FieldsKey betaFields, boolean beta, AlphaMeta candidate) {
            Map<FieldsKey, FieldAlphas> map = beta ?
                    dataBeta
                    :
                    dataAlpha;

            return map.computeIfAbsent(betaFields, FieldAlphas::new).getCreate(candidate);
        }

        @Override
        public TypeAlphas copyOf() {
            return new TypeAlphas(this);
        }
    }

    private static class AlphaMeta {
        private final int[] alphaIndices;
        private final boolean[] requiredValues;

        AlphaMeta(int[] alphaIndices, boolean[] requiredValues) {
            this.alphaIndices = alphaIndices;
            this.requiredValues = requiredValues;
        }
    }
}
