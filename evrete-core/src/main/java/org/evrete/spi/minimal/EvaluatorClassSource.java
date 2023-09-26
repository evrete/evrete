package org.evrete.spi.minimal;

import org.evrete.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicLong;

import static org.evrete.Configuration.CONDITION_BASE_CLASS;

class EvaluatorClassSource {
    private final static AtomicLong JAVA_CLASS_COUNTER = new AtomicLong();

    private static final String JAVA_EVALUATOR_TEMPLATE = "package %s;\n" +
            "%s\n" +
            "\n" +
            "public final class %s extends %s {\n" +
            "    public static final java.lang.invoke.MethodHandle HANDLE;\n" +
            "\n" +
            "    static {\n" +
            "        try {\n" +
            "            HANDLE = java.lang.invoke.MethodHandles.lookup().findStatic(%s.class, \"__$test\", java.lang.invoke.MethodType.methodType(boolean.class, %s));\n" +
            "        } catch (Exception e) {\n" +
            "            throw new IllegalStateException(e);\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    private static boolean __$testInner(%s) {\n" +
            "        return %s;\n" +
            "    }\n" +
            "\n" +
            "    public static boolean __$test(%s) {\n" +
            "        return %s\n" +
            "    }\n\n" +
            "    //IMPORTANT LINE BELOW, IT IS USED IN SOURCE/SIGNATURE COMPARISON\n" +
            "    //%s\n" +
            "}\n";

    private final String fullJavaSource;
    private final String comparableClassSource;
    private final FieldReference[] descriptor;
    private final LiteralExpression expression;
    private final String className;

    public EvaluatorClassSource(RuntimeContext<?> context, LiteralExpression expression, final StringLiteralEncoder encoder, List<ConditionStringTerm> terms) throws IllegalArgumentException {
        this.expression = expression;
        List<ConditionStringTerm> uniqueReferences = new ArrayList<>();
        List<FieldReference> descriptorBuilder = new ArrayList<>();

        String baseClassName = context
                .getConfiguration()
                .getProperty(CONDITION_BASE_CLASS, BaseConditionClass.class.getName());

        Imports imports = context.getImports();


        String encodedExpression = encoder.getEncoded().value;
        int accumulatedShift = 0;
        int castVarIndex = 0;
        StringJoiner argTypes = new StringJoiner(", ");
        StringJoiner argCasts = new StringJoiner(", ");
        StringJoiner methodArgs = new StringJoiner(", ");
        for (ConditionStringTerm term : terms) {
            String original = encodedExpression.substring(term.start + accumulatedShift, term.end + accumulatedShift);
            String javaArgVar = term.varName;
            String before = encodedExpression.substring(0, term.start + accumulatedShift);
            String after = encodedExpression.substring(term.end + accumulatedShift);
            encodedExpression = before + javaArgVar + after;
            accumulatedShift += javaArgVar.length() - original.length();


            if (!uniqueReferences.contains(term)) {
                //Build the reference
                descriptorBuilder.add(term);
                //Prepare the corresponding source code vars
                Class<?> fieldType = term.field().getValueType();

                argTypes.add(term.type().getType().getName() + "/" + term.field().getName());
                argCasts.add("(" + fieldType.getCanonicalName() + ") values.apply(" + castVarIndex + ")");
                methodArgs.add(fieldType.getCanonicalName() + " " + javaArgVar);
                castVarIndex++;
                // Mark as processed
                uniqueReferences.add(term);
            }
        }

        // Adding imports
        StringBuilder importsBuilder = new StringBuilder(1024);
        imports.asJavaImportStatements(importsBuilder);

        String replaced = encoder.unwrapLiterals(encodedExpression);

        String pkg = this.getClass().getPackage().getName() + ".compiled";
        String classSimpleName = "Condition" + JAVA_CLASS_COUNTER.incrementAndGet();
        this.className = pkg + "." + classSimpleName;
        this.fullJavaSource = String.format(
                JAVA_EVALUATOR_TEMPLATE,
                pkg,
                importsBuilder,
                classSimpleName,
                baseClassName,
                classSimpleName,
                IntToValue.class.getName() + ".class",
                methodArgs,
                replaced,
                IntToValue.class.getName() + " values",
                "__$testInner(" + argCasts + ");",
                "fields in use: " + argTypes
        );

        if(descriptorBuilder.isEmpty()) {
            throw new IllegalArgumentException("No field references were resolved in the '" + expression.getSource() + "'");
        } else {
            this.comparableClassSource = fullJavaSource.replaceAll(classSimpleName, "CLASS_STUB");
            this.descriptor = descriptorBuilder.toArray(FieldReference.ZERO_ARRAY);
        }
    }

    public String getClassName() {
        return className;
    }

    public String getFullJavaSource() {
        return fullJavaSource;
    }

    public LiteralExpression getExpression() {
        return expression;
    }

    public String getComparableClassSource() {
        return comparableClassSource;
    }

    public FieldReference[] getDescriptor() {
        return descriptor;
    }
}
