package org.evrete.spi.minimal;

import org.evrete.api.Evaluator;
import org.evrete.api.IntToValue;
import org.evrete.api.LogicallyComparable;
import org.evrete.runtime.builder.FieldReference;
import org.evrete.util.NextIntSupplier;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

class EvaluatorCompiler {
    private static final String JAVA_EVALUATOR_TEMPLATE = "package %s;\n" +
            "\n" +
            "public class %s {\n" +
            "    public static final java.lang.invoke.MethodHandle HANDLE;\n" +
            "\n" +
            "    static {\n" +
            "        try {\n" +
            "            HANDLE = java.lang.invoke.MethodHandles.lookup().findStatic(%s.class, \"test\", java.lang.invoke.MethodType.methodType(boolean.class, %s));\n" +
            "        } catch (Exception e) {\n" +
            "            throw new IllegalStateException(e);\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    private static boolean testInner(%s) {\n" +
            "        return %s;\n" +
            "    }\n" +
            "\n" +
            "    public static boolean test(%s) {\n" +
            "        return %s\n" +
            "    }\n\n" +
            "    //IMPORTANT LINE BELOW, IT IS USED IN SOURCE/SIGNATURE COMPARISON\n" +
            "    //%s\n" +
            "}\n";


    private final static NextIntSupplier javaClassCounter = new NextIntSupplier();

    private final ClassLoader classLoader;

    EvaluatorCompiler(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }


    private MethodHandle compileExpression(String className, String classJavaSource) {
        try {
            JcCompiler compiler = new JcCompiler(classLoader);
            Class<?> compiledClass = compiler.compile(className, classJavaSource);
            return (MethodHandle) compiledClass.getDeclaredField("HANDLE").get(null);
        } catch (JcCompilationException e) {
            throw e;
        } catch (Exception e) {
            throw new JcCompilationException(e);
        }
    }

    Evaluator buildExpression(StringLiteralRemover remover, String strippedExpression, List<ConditionStringTerm> terms) {

        int accumulatedShift = 0;
        StringJoiner argClasses = new StringJoiner(", ");
        StringJoiner argTypes = new StringJoiner(", ");
        StringJoiner argCasts = new StringJoiner(", ");
        int castVarIndex = 0;
        StringJoiner methodArgs = new StringJoiner(", ");
        List<ConditionStringTerm> uniqueReferences = new ArrayList<>();
        List<FieldReference> descriptorBuilder = new ArrayList<>();

        for (ConditionStringTerm term : terms) {
            //JavaSourceFieldReference replacement = term.decoded;//resolvedExpressions.get(term);
            String original = strippedExpression.substring(term.start + accumulatedShift, term.end + accumulatedShift);
            String javaArgVar = term.varName;
            String before = strippedExpression.substring(0, term.start + accumulatedShift);
            String after = strippedExpression.substring(term.end + accumulatedShift);
            strippedExpression = before + javaArgVar + after;
            accumulatedShift += javaArgVar.length() - original.length();


            if (!uniqueReferences.contains(term)) {
                //Build the reference
                //FieldReferenceI ref = replacement;
                descriptorBuilder.add(term);
                //Prepare the corresponding source code vars
                Class<?> fieldType = term.field().getValueType();

                argTypes.add(term.type().getType().getName() + "/" + term.field().getName());
                argCasts.add("(" + fieldType.getName() + ") values.apply(" + castVarIndex + ")");
                argClasses.add(fieldType.getName() + ".class");
                methodArgs.add(fieldType.getName() + " " + javaArgVar);
                castVarIndex++;
                // Mark as processed
                uniqueReferences.add(term);
            }
        }

        String replaced = remover.unwrapLiterals(strippedExpression);


        String pkg = this.getClass().getPackage().getName() + ".compiled";
        String clazz = "Condition" + javaClassCounter.next();
        String className = pkg + "." + clazz;
        String classJavaSource = String.format(
                JAVA_EVALUATOR_TEMPLATE,
                pkg,
                clazz,
                clazz,
                IntToValue.class.getName() + ".class",
                methodArgs.toString(),
                replaced,
                IntToValue.class.getName() + " values",
                "testInner(" + argCasts.toString() + ");",
                "fields in use: " + argTypes.toString()
        );

        String comparableClassSource = classJavaSource.replaceAll(clazz, "CLASS_STUB");

        FieldReference[] descriptor = descriptorBuilder.toArray(FieldReference.ZERO_ARRAY);
        if(descriptor.length == 0) throw new IllegalStateException("No field references were resolved.");
        MethodHandle methodHandle = compileExpression(className, classJavaSource);
        return new EvaluatorImpl(methodHandle, remover.getOriginal(), comparableClassSource, descriptor);

    }

    private static class EvaluatorImpl implements Evaluator {
        private final FieldReference[] descriptor;
        private final MethodHandle methodHandle;
        private final String original;
        private final String comparableClassSource;

        EvaluatorImpl(MethodHandle methodHandle, String original, String comparableClassSource, FieldReference[] descriptor) {
            this.descriptor = descriptor;
            this.original = original;
            this.comparableClassSource = comparableClassSource;
            this.methodHandle = methodHandle;
        }

        @Override
        public int compare(LogicallyComparable other) {
            if (other instanceof EvaluatorImpl) {
                EvaluatorImpl o = (EvaluatorImpl) other;
                if (o.descriptor.length == 1 && this.descriptor.length == 1 && o.comparableClassSource.equals(this.comparableClassSource)) {
                    return RELATION_EQUALS;
                }
            }

            return LogicallyComparable.RELATION_NONE;
        }

        @Override
        public FieldReference[] descriptor() {
            return descriptor;
        }

        @Override
        public boolean test(IntToValue values) {
            try {
                return (boolean) methodHandle.invoke(values);
            } catch (Throwable t) {
                Object[] args = new Object[descriptor.length];
                for (int i = 0; i < args.length; i++) {
                    args[i] = values.apply(i);
                }
                throw new IllegalStateException("Evaluation exception at '" + original + "': " + Arrays.toString(descriptor) + " -> " + Arrays.toString(args), t);
            }
        }

        @Override
        public String toString() {
            return "\"" + original + "\"";
        }
    }

}
