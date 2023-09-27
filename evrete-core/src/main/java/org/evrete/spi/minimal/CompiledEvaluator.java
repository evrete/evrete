package org.evrete.spi.minimal;

import org.evrete.api.*;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;

class CompiledEvaluator implements LiteralEvaluator {
    private final FieldReference[] descriptor;
    private final MethodHandle methodHandle;
    private final LiteralExpression source;
    private final String originalCondition;
    private final String javaClassSource;
    private final String comparableClassSource;

    CompiledEvaluator(MethodHandle methodHandle, EvaluatorClassSource source) {
        this.source = source.getExpression();
        this.descriptor = source.getDescriptor();
        this.originalCondition = source.getExpression().getSource();
        this.javaClassSource = source.getSource();
        this.comparableClassSource = source.getComparableClassSource();
        this.methodHandle = methodHandle;
    }

    @Override
    public LiteralExpression getSource() {
        return source;
    }

    String getJavaSource() {
        return javaClassSource;
    }

    @Override
    public int compare(Evaluator other) {
        if (other instanceof CompiledEvaluator) {
            CompiledEvaluator o = (CompiledEvaluator) other;
            if (o.descriptor.length == 1 && this.descriptor.length == 1 && o.comparableClassSource.equals(this.comparableClassSource)) {
                return RELATION_EQUALS;
            }
        }

        return LiteralEvaluator.super.compare(other);
    }

    @Override
    public FieldReference[] descriptor() {
        return descriptor;
    }

    @Override
    public boolean test(IntToValue values) {
        try {
            return (boolean) methodHandle.invoke(values);
        } catch (SecurityException t) {
            throw t;
        } catch (Throwable t) {
            Object[] args = new Object[descriptor.length];
            for (int i = 0; i < args.length; i++) {
                args[i] = values.apply(i);
            }
            throw new IllegalStateException("Evaluation exception at '" + originalCondition + "', arguments: " + Arrays.toString(descriptor) + " -> " + Arrays.toString(args), t);
        }
    }

    @Override
    public String toString() {
        return "\"" + originalCondition + "\"";
    }
}
