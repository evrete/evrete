package org.evrete.runtime;

import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.runtime.evaluation.EvaluatorWrapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

abstract class RuntimeMetaData<C extends RuntimeContext<C>> implements RuntimeContext<C>, MetaChangeListener {
    private static final Comparator<ActiveField> DEFAULT_FIELD_COMPARATOR = Comparator.comparing(ActiveField::getValueIndex);
    private final Imports imports;
    private final Map<String, Object> properties;
    private final ArrayOf<TypeMemoryMetaData> typeMetas;
    private final ArrayOf<FieldsKey> memoryKeys;
    private final Evaluators evaluators;
    private TypeResolver typeResolver;
    private ClassLoader classLoader;

    RuntimeMetaData(KnowledgeService service) {
        this.classLoader = service.getClassLoader();
        this.typeResolver = service.getTypeResolverProvider().instance(this);
        this.imports = service.getConfiguration().getImports().copyOf();
        this.typeMetas = new ArrayOf<>(TypeMemoryMetaData.class);
        this.memoryKeys = new ArrayOf<>(FieldsKey.class);
        this.properties = new ConcurrentHashMap<>();
        this.evaluators = new Evaluators();
    }

    RuntimeMetaData(RuntimeMetaData<?> parent) {
        this.classLoader = parent.classLoader;
        this.typeResolver = parent.typeResolver.copyOf();
        this.imports = parent.imports.copyOf();
        this.evaluators = parent.evaluators.copyOf();
        this.properties = new ConcurrentHashMap<>(parent.properties);
        this.memoryKeys = new ArrayOf<>(parent.memoryKeys);
        this.typeMetas = new ArrayOf<>(TypeMemoryMetaData.class);

        MetaChangeListener listener = this;
        parent.typeMetas
                .forEach(
                        (meta, i) -> RuntimeMetaData.this.typeMetas.set(i, meta.copyOf(this.evaluators, listener))
                );
    }

    public Evaluators getEvaluators() {
        return evaluators;
    }

    @Override
    public ClassLoader getClassLoader() {
        return Objects.requireNonNull(classLoader);
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public final void wrapTypeResolver(TypeResolverWrapper wrapper) {
        this.typeResolver = wrapper;
    }

    @Override
    public final TypeResolver getTypeResolver() {
        return typeResolver;
    }

    @Override
    public EvaluatorHandle addEvaluator(Evaluator evaluator, double complexity) {
        return evaluators.save(evaluator, complexity);
    }

    EvaluatorWrapper getEvaluatorWrapper(EvaluatorHandle handle) {
        return evaluators.get(handle);
    }

    @Override
    public void replaceEvaluator(EvaluatorHandle handle, Evaluator newEvaluator) {
        evaluators.replace(handle, newEvaluator);
    }

    @Override
    public void addListener(EvaluationListener listener) {
        this.evaluators.addListener(listener);
    }

    @Override
    public void removeListener(EvaluationListener listener) {
        this.evaluators.removeListener(listener);
    }

    TypeMemoryMetaData getTypeMeta(int type) {
        return typeMetas.computeIfAbsent(type, k -> new TypeMemoryMetaData(type, evaluators, RuntimeMetaData.this));
    }

    private ActiveField getCreateActiveField(TypeField field) {
        TypeMemoryMetaData meta = getTypeMeta(field.getDeclaringType().getId());
        return meta.getCreate(field);
    }

    synchronized AlphaBucketMeta buildAlphaMask(FieldsKey key, Set<EvaluatorHandle> alphaEvaluators) {
        TypeMemoryMetaData typeMeta = getTypeMeta(key.type());
        return typeMeta.buildAlphaMask(key, alphaEvaluators);
    }

    private ActiveField[] getCreate(Set<TypeField> fields) {
        Set<ActiveField> set = new HashSet<>(fields.size());
        fields.forEach(f -> set.add(getCreateActiveField(f)));
        ActiveField[] activeFields = set.toArray(ActiveField.ZERO_ARRAY);
        Arrays.sort(activeFields, DEFAULT_FIELD_COMPARATOR);
        return activeFields;
    }

    FieldsKey getCreateMemoryKey(Type<?> type, Set<TypeField> fields) {
        ActiveField[] activeFields;
        if (fields.isEmpty()) {
            activeFields = ActiveField.ZERO_ARRAY;
        } else {
            activeFields = getCreate(fields);
        }

        // Scanning existing data
        for (int i = 0; i < memoryKeys.data.length; i++) {
            FieldsKey key = memoryKeys.getChecked(i);
            if (Arrays.equals(key.getFields(), activeFields) && type.getId() == key.type()) {
                return key;
            }
        }

        // No match found, creating new key
        int newId = memoryKeys.data.length;
        FieldsKey newKey = new FieldsKey(newId, type, activeFields);
        memoryKeys.set(newId, newKey);
        return newKey;
    }

    @SuppressWarnings("unchecked")
    public final C addImport(RuleScope scope, String imp) {
        this.imports.add(scope, imp);
        return (C) this;
    }

    @Override
    public Imports getImports() {
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

}
