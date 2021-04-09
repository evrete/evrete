package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.builder.FactTypeBuilder;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.runtime.evaluation.AlphaEvaluator;
import org.evrete.runtime.evaluation.EvaluatorWrapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

abstract class RuntimeMetaData<C extends RuntimeContext<C>> implements RuntimeContext<C> {
    private static final Comparator<ActiveField> DEFAULT_FIELD_COMPARATOR = Comparator.comparing(ActiveField::getValueIndex);
    private final Imports imports;
    private final Map<String, Object> properties;
    private final ArrayOf<TypeMeta> typeMetas;
    private final ArrayOf<FieldKeyMeta> keyMetas;
    private final ArrayOf<FieldsKey> memoryKeys;

    RuntimeMetaData() {
        this.imports = new Imports();
        this.typeMetas = new ArrayOf<>(TypeMeta.class);
        this.memoryKeys = new ArrayOf<>(FieldsKey.class);
        this.properties = new ConcurrentHashMap<>();
        this.keyMetas = new ArrayOf<>(FieldKeyMeta.class);
    }

    RuntimeMetaData(RuntimeMetaData<?> parent) {
        this.imports = parent.imports.copyOf();
        this.properties = new ConcurrentHashMap<>(parent.properties);
        this.memoryKeys = new ArrayOf<>(parent.memoryKeys);
        this.typeMetas = new ArrayOf<>(TypeMeta.class);
        parent.typeMetas
                .forEach(
                        (meta, i) -> RuntimeMetaData.this.typeMetas.set(i, meta.copyOf())
                );

        this.keyMetas = new ArrayOf<>(FieldKeyMeta.class);
        parent.keyMetas
                .forEach(
                        (meta, i) -> RuntimeMetaData.this.keyMetas.set(i, meta.copyOf())
                );
    }

    protected abstract void onNewActiveField(TypeMemoryState newState, ActiveField newField);

    public abstract void onNewAlphaBucket(TypeMemoryState newState, FieldsKey key, AlphaBucketMeta meta);

    void forEachAlphaCondition(Consumer<AlphaEvaluator> consumer) {
        typeMetas.forEach(meta -> {
            for (AlphaEvaluator evaluator : meta.alphaEvaluators) {
                consumer.accept(evaluator);
            }
        });
    }

    private TypeMeta getTypeMeta(Type<?> type) {
        return typeMetas.computeIfAbsent(type.getId(), k -> new TypeMeta(type));
    }

    private FieldKeyMeta getKeyMeta(FieldsKey key) {
        return keyMetas.computeIfAbsent(key.getId(), k -> new FieldKeyMeta());
    }

    private ActiveField getCreate(TypeField field) {
        TypeMeta meta = getTypeMeta(field.getDeclaringType());
        return meta.getCreate(field, newField -> {
            TypeMemoryState newState = meta.asState();
            RuntimeMetaData.this.onNewActiveField(newState, newField);
        });
    }

    synchronized AlphaBucketMeta buildAlphaMask(FieldsKey key, Set<EvaluatorWrapper> alphaEvaluators) {
        TypeMeta typeMeta = getTypeMeta(key.getType());
        AlphaEvaluator[] existing = typeMeta.alphaEvaluators;
        Set<AlphaEvaluator.Match> matches = new HashSet<>();
        for (EvaluatorWrapper wrapper : alphaEvaluators) {
            AlphaEvaluator.Match match = AlphaEvaluator.search(existing, wrapper);
            if (match == null) {
                // No such evaluator, creating a new one
                AlphaEvaluator alphaEvaluator = typeMeta.append(wrapper, convertDescriptor(wrapper.descriptor()));
                existing = typeMeta.alphaEvaluators;
                matches.add(new AlphaEvaluator.Match(alphaEvaluator, true));
            } else {
                matches.add(match);
            }
        }

        // Now that all evaluators are matched,
        // their unique combinations are converted to a alpha bucket meta-data
        FieldKeyMeta fieldKeyMeta = getKeyMeta(key);

        for (AlphaBucketMeta meta : fieldKeyMeta.alphaBuckets) {
            if (meta.sameKey(matches)) {
                return meta;
            }
        }

        // Not found creating a new one
        int bucketIndex = fieldKeyMeta.alphaBuckets.length;
        AlphaBucketMeta newMeta = AlphaBucketMeta.factory(bucketIndex, matches);
        fieldKeyMeta.alphaBuckets = Arrays.copyOf(fieldKeyMeta.alphaBuckets, fieldKeyMeta.alphaBuckets.length + 1);
        fieldKeyMeta.alphaBuckets[bucketIndex] = newMeta;

        onNewAlphaBucket(typeMeta.asState(), key, newMeta);
        return newMeta;
    }

    private ActiveField[] convertDescriptor(FieldReference[] descriptor) {
        ActiveField[] converted = new ActiveField[descriptor.length];
        for (int i = 0; i < descriptor.length; i++) {
            TypeField field = descriptor[i].field();
            ActiveField activeField = getCreate(field);
            converted[i] = activeField;
        }

        return converted;
    }

    private ActiveField[] getCreate(Set<TypeField> fields) {
        Set<ActiveField> set = new HashSet<>(fields.size());
        fields.forEach(f -> set.add(getCreate(f)));
        ActiveField[] activeFields = set.toArray(ActiveField.ZERO_ARRAY);
        Arrays.sort(activeFields, DEFAULT_FIELD_COMPARATOR);
        return activeFields;
    }

    FieldsKey getCreateMemoryKey(FactTypeBuilder builder) {
        Set<TypeField> fields = builder.getBetaTypeFields();
        ActiveField[] activeFields;
        Type<?> type = builder.getType();
        if (fields.isEmpty()) {
            activeFields = ActiveField.ZERO_ARRAY;
        } else {
            activeFields = getCreate(fields);
            Set<Type<?>> distinctTypes = Arrays
                    .stream(activeFields)
                    .map(ActiveField::getDeclaringType)
                    .collect(Collectors.toSet());
            assert distinctTypes.size() == 1 && distinctTypes.iterator().next().equals(type);
        }

        // Scanning existing data
        for (int i = 0; i < memoryKeys.data.length; i++) {
            FieldsKey key = memoryKeys.getChecked(i);
            if (Arrays.equals(key.getFields(), activeFields) && type.equals(key.getType())) {
                return key;
            }
        }

        // No match found, creating new key
        int newId = memoryKeys.data.length;
        FieldsKey newKey = new FieldsKey(newId, type, activeFields);
        memoryKeys.set(newId, newKey);
        return newKey;
    }

    ActiveField[] getActiveFields(Type<?> t) {
        return getTypeMeta(t).activeFields;
    }

    TypeMemoryState getActiveSate(Type<?> t) {
        return getTypeMeta(t).asState();
    }

    AlphaEvaluator[] getAlphaEvaluators(Type<?> t) {
        return getTypeMeta(t).alphaEvaluators;
    }

    public RuntimeContext<?> addImport(RuleScope scope, String imp) {
        this.imports.add(scope, imp);
        return this;
    }

    public final Set<String> getImports(RuleScope... scopes) {
        return imports.get(scopes);
    }

    public Imports getImportsData() {
        return imports;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final C set(String property, Object value) {
        this.properties.put(property, value);
        return (C) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <T> T get(String property) {
        return (T) properties.get(property);
    }

    @SuppressWarnings("unused")
    @Override
    public final Collection<String> getPropertyNames() {
        return properties.keySet();
    }

    public abstract ExpressionResolver getExpressionResolver();

    @FunctionalInterface
    public interface NewActiveFieldListener {

        void onNew(ActiveField newField);
    }

    private static class FieldKeyMeta implements Copyable<FieldKeyMeta> {
        private AlphaBucketMeta[] alphaBuckets;

        FieldKeyMeta() {
            this.alphaBuckets = new AlphaBucketMeta[0];
        }

        FieldKeyMeta(FieldKeyMeta other) {
            this.alphaBuckets = Arrays.copyOf(other.alphaBuckets, other.alphaBuckets.length);
        }

        @Override
        public FieldKeyMeta copyOf() {
            return new FieldKeyMeta(this);
        }
    }

    private static class TypeMeta implements Copyable<TypeMeta> {
        private ActiveField[] activeFields;
        private AlphaEvaluator[] alphaEvaluators;
        private final Type<?> type;


        TypeMeta(Type<?> type) {
            this.activeFields = ActiveField.ZERO_ARRAY;
            this.alphaEvaluators = new AlphaEvaluator[0];
            this.type = type;
        }

        TypeMemoryState asState() {
            return new TypeMemoryState(type, activeFields, alphaEvaluators);
        }

        TypeMeta(TypeMeta other) {
            this.activeFields = Arrays.copyOf(other.activeFields, other.activeFields.length);
            this.alphaEvaluators = Arrays.copyOf(other.alphaEvaluators, other.alphaEvaluators.length);
            this.type = other.type;
        }

        private synchronized AlphaEvaluator append(EvaluatorWrapper wrapper, ActiveField[] descriptor) {
            int newId = this.alphaEvaluators.length;
            AlphaEvaluator alphaEvaluator = new AlphaEvaluator(newId, wrapper, descriptor);
            this.alphaEvaluators = Arrays.copyOf(this.alphaEvaluators, this.alphaEvaluators.length + 1);
            this.alphaEvaluators[newId] = alphaEvaluator;
            return alphaEvaluator;
        }

        @Override
        public TypeMeta copyOf() {
            return new TypeMeta(this);
        }

        private synchronized ActiveField getCreate(TypeField field, NewActiveFieldListener listener) {
            for (ActiveField af : activeFields) {
                if (af.getName().equals(field.getName())) {
                    return af;
                }
            }
            // Create and store new instance
            int id = activeFields.length;
            ActiveField af = new ActiveFieldImpl(field, id);
            this.activeFields = Arrays.copyOf(this.activeFields, id + 1);
            this.activeFields[id] = af;
            listener.onNew(af);
            return af;
        }

    }
}
