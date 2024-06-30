package org.evrete.spi.minimal;

import org.evrete.api.*;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.spi.JavaSourceCompiler;
import org.evrete.api.spi.LiteralSourceCompiler;
import org.evrete.util.CommonUtils;
import org.evrete.util.CompilationException;

import java.lang.invoke.MethodHandle;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.evrete.Configuration.RULE_BASE_CLASS;
import static org.evrete.Configuration.SPI_LHS_STRIP_WHITESPACES;

public class DefaultLiteralSourceCompiler extends LeastImportantServiceProvider implements LiteralSourceCompiler {
    private static final String TAB = "  ";
    private static final String RHS_CLASS_NAME = "Rhs";
    private static final String RHS_INSTANCE_VAR = "ACTION";

    private static final AtomicInteger classCounter = new AtomicInteger(0);
    static final String CLASS_PACKAGE = DefaultLiteralSourceCompiler.class.getPackage().getName() + ".compiled";

    @Override
    public <S extends RuleLiteralData<R, C>, R extends Rule, C extends LiteralPredicate> Collection<RuleCompiledSources<S, R, C>> compile(RuntimeContext<?> context, Collection<S> sources) throws CompilationException {
        // Return if there's nothing to compile
        if (sources.isEmpty()) {
            return Collections.emptyList();
        }

        String stripFlag = context.getConfiguration().getProperty(SPI_LHS_STRIP_WHITESPACES);

        if (stripFlag == null) {
            try {
                // Try compiling with stripped whitespaces
                return compile(context, sources, true);
            } catch (CompilationException e) {
                // Compile literals as-is
                return compile(context, sources, false);
            }
        } else {
            return compile(context, sources, Boolean.parseBoolean(stripFlag));
        }
    }

    private <S extends RuleLiteralData<R, C>, R extends Rule, C extends LiteralPredicate> Collection<RuleCompiledSources<S, R, C>> compile(RuntimeContext<?> context, Collection<S> sources, boolean stripWhitespaces) throws CompilationException {
        JavaSourceCompiler compiler = context.getSourceCompiler();

        Collection<RuleSource<S, R, C>> javaSources = sources.stream()
                .map(o -> new RuleSource<>(o, context, stripWhitespaces))
                .collect(Collectors.toList());

        Collection<JavaSourceCompiler.Result<RuleSource<S, R, C>>> result = compiler.compile(javaSources);

        return result
                .stream()
                .map(compiledSource -> {
                    Class<?> ruleClass = compiledSource.getCompiledClass();
                    return new RuleCompiledSourcesImpl<>(ruleClass, compiledSource.getSource(), compiledSource.getSource().javaSource);
                })
                .collect(Collectors.toList());
    }

    public static class RuleSource<S extends RuleLiteralData<R, C>, R extends Rule, C extends LiteralPredicate> implements JavaSourceCompiler.ClassSource {
        private final String className;
        private final String classSimpleName;

        private final S delegate;
        private final Imports imports;
        private final RhsSource rhsSource;

        private final String javaSource;
        private final Collection<ConditionSource<C>> conditionSources;

        RuleSource(S delegate, RuntimeContext<?> context, boolean stripWhitespaces) {
            this.delegate = delegate;
            this.imports = context.getImports();
            this.classSimpleName = "Rule" + classCounter.incrementAndGet();
            this.className = CLASS_PACKAGE + "." + classSimpleName;

            AtomicInteger conditionCounter = new AtomicInteger();
            this.conditionSources = delegate.conditions()
                    .stream()
                    .map(s -> new ConditionSource<>(delegate.getRule(), "condition" + conditionCounter.incrementAndGet(), this.classSimpleName, s, stripWhitespaces))
                    .collect(Collectors.toList());

            String rhs = delegate.rhs();
            this.rhsSource = rhs == null ? null : new RhsSource(delegate.getRule(), rhs);
            this.javaSource = this.buildSource();
        }

        private String buildSource() {
            StringBuilder sb = new StringBuilder(4096);
            // Class header
            appendHeader(sb);

            // Class RHS body
            if (this.rhsSource != null) {
                this.rhsSource.appendClassVar(sb);
            }

            // Class conditions declarations
            for (ConditionSource<C> source : this.conditionSources) {
                sb.append(TAB);
                source.appendDeclaration(sb);
            }

            // Class conditions definitions
            if (!this.conditionSources.isEmpty()) {
                sb.append("\n").append(TAB).append("static {\n");
                sb.append(TAB).append(TAB).append("try {\n");
                for (ConditionSource<C> source : this.conditionSources) {
                    sb.append(TAB);
                    sb.append(TAB);
                    sb.append(TAB);
                    source.appendDefinition(sb);
                }
                sb.append(TAB).append(TAB).append("} catch (Exception e) {\n");
                sb.append(TAB).append(TAB).append(TAB).append("throw new IllegalStateException(e);\n");
                sb.append(TAB).append(TAB).append("}\n");
                sb.append(TAB).append("}\n");
            }

            // Class conditions methods
            for (ConditionSource<C> source : this.conditionSources) {
                source.appendHandleMethod(sb);
                source.appendInnerMethod(sb);
                sb.append("\n");
            }

            // Class RHS body
            if (this.rhsSource != null) {
                this.rhsSource.appendClassBody(sb);
            }

            // Class footer
            appendFooter(sb);
            return sb.toString();
        }

        @Override
        public String binaryName() {
            return className;
        }

        @Override
        public String getSource() {
            return javaSource;
        }

        private void appendHeader(StringBuilder target) {
            // Declare package
            target.append("package ").append(CLASS_PACKAGE).append(";\n\n");

            // Declare imports
            imports.asJavaImportStatements(target);

            // Declare class
            String baseClassName = delegate.getRule().get(RULE_BASE_CLASS, BaseRuleClass.class.getCanonicalName());

            target.append("public final class ")
                    .append(classSimpleName)
                    .append(" extends ")
                    .append(baseClassName)
                    .append(" {\n");

        }

        private void appendFooter(StringBuilder target) {
            target.append("\n}\n");
        }
    }

    private static class ConditionSource<C extends LiteralPredicate> {
        private static final String DECLARATION_TEMPLATE =
                "public static final java.lang.invoke.MethodHandle %s;\n";
        private static final String DEFINITION_TEMPLATE =
                "%s = java.lang.invoke.MethodHandles.lookup().findStatic(%s.class, \"%s\", java.lang.invoke.MethodType.methodType(boolean.class, %s));\n";
        private static final String INNER_METHOD_TEMPLATE = "\n" +
                "  private static boolean %sInner(%s) {\n" +
                "    return %s;\n" +
                "  }\n";
        private static final String HANDLE_METHOD_TEMPLATE = "\n" +
                "  public static boolean %s(%s) {\n" +
                "    return %sInner(%s);\n" +
                "  }\n";

        final C source;
        final String methodName;
        final String handleName;
        final String className;
        private final String replaced;
        private final StringJoiner methodArgs;
        private final StringJoiner argCasts;
        private final LhsField.Array<String, TypeField> resolvedFields;

        public ConditionSource(Rule rule, String name, String className, C source, boolean stripWhitespaces) {
            this.className = className;
            this.source = source;
            this.methodName = name;
            this.handleName = name.toUpperCase() + "_HANDLE";
            StringLiteralEncoder encoder = StringLiteralEncoder.of(source.getSource(), stripWhitespaces);

            final List<ConditionStringTerm> terms = ConditionStringTerm.resolveTerms(encoder.getEncoded());

            List<LhsField<String, String>> uniqueReferences = new ArrayList<>();
            List<LhsField<String, TypeField>> descriptorBuilder = new ArrayList<>();

            String encodedExpression = encoder.getEncoded().value;
            int accumulatedShift = 0;
            int castVarIndex = 0;
            this.argCasts = new StringJoiner(", ");
            this.methodArgs = new StringJoiner(", ");
            for (ConditionStringTerm term : terms) {
                String original = encodedExpression.substring(term.start + accumulatedShift, term.end + accumulatedShift);
                String javaArgVar = term.varName;
                String before = encodedExpression.substring(0, term.start + accumulatedShift);
                String after = encodedExpression.substring(term.end + accumulatedShift);
                encodedExpression = before + javaArgVar + after;
                accumulatedShift += javaArgVar.length() - original.length();

                LhsField<String, String> ref = term.ref;

                if (!uniqueReferences.contains(ref)) {

                    LhsField<String, TypeField> resolvedField = CommonUtils.toTypeField(ref, rule);

                    // Get the referenced field's type
                    String canonicalFieldType = resolvedField
                            .field()
                            .getValueType()
                            .getCanonicalName();

                    //Build the reference
                    descriptorBuilder.add(resolvedField);

                    //argTypes.add(term.type().getType().getName() + "/" + term.field().getName());
                    argCasts.add("(" + canonicalFieldType + ") values.apply(" + castVarIndex + ")");
                    methodArgs.add(canonicalFieldType + " " + javaArgVar);
                    castVarIndex++;
                    // Mark as processed
                    uniqueReferences.add(ref);
                }
            }

            this.replaced = encoder.unwrapLiterals(encodedExpression);
            this.resolvedFields = new LhsField.Array<>(descriptorBuilder);
        }

        void appendDeclaration(StringBuilder target) {
            target.append(String.format(DECLARATION_TEMPLATE, handleName));
        }

        void appendHandleMethod(StringBuilder target) {
            target.append(String.format(HANDLE_METHOD_TEMPLATE, methodName, IntToValue.class.getName() + " values", methodName, argCasts));
        }

        void appendInnerMethod(StringBuilder target) {
            target.append(String.format(INNER_METHOD_TEMPLATE, methodName, methodArgs, replaced));
        }

        void appendDefinition(StringBuilder target) {
            target.append(String.format(DEFINITION_TEMPLATE, handleName, className, methodName, IntToValue.class.getName() + ".class"));
        }
    }

    private static class RhsSource {
        @NonNull
        final String rhs;
        final Rule rule;
        final StringJoiner methodArgs;
        final StringJoiner args;

        RhsSource(Rule rule, @NonNull String rhs) {
            this.rule = rule;
            this.rhs = rhs;
            this.methodArgs = new StringJoiner(", ");
            this.args = new StringJoiner(", ");
            for (NamedType t : rule.getDeclaredFactTypes()) {
                methodArgs.add(t.getType().getJavaClass().getCanonicalName() + " " + t.getVarName());
                args.add(t.getVarName());
            }
        }

        void appendClassVar(StringBuilder target) {
            target
                    .append(TAB)
                    .append("public static final " + RHS_CLASS_NAME + " ")
                    .append(RHS_INSTANCE_VAR + " = new " + RHS_CLASS_NAME + "();")
                    .append("\n")
            ;
        }

        void appendClassBody(StringBuilder target) {
            target
                    .append("\n")
                    .append(TAB)
                    .append("public static class " + RHS_CLASS_NAME + " extends ")
                    .append(AbstractLiteralRhs.class.getName())
                    .append(" {\n\n")
                    .append(TAB).append(TAB)
                    .append("@Override\n")
                    .append(TAB).append(TAB)
                    .append("protected final void doRhs() {\n")
            ;

            // Assign vars
            for (NamedType t : rule.getDeclaredFactTypes()) {
                target
                        .append(TAB).append(TAB).append(TAB)
                        .append(t.getType().getJavaClass().getCanonicalName()).append(" ")
                        .append(t.getVarName())
                        .append(" = ")
                        .append("get(\"")
                        .append(t.getVarName())
                        .append("\");\n");
            }

            // Inner method call
            target
                    .append(TAB).append(TAB).append(TAB)
                    .append("this.doRhs(")
                    .append(this.args)
                    .append(");\n")
                    .append(TAB).append(TAB)
                    .append("}\n\n")
            ;

            // Inner method declaration
            target.append(TAB).append(TAB)
                    .append("private void doRhs(")
                    .append(this.methodArgs)
                    .append(") {\n")
            ;

            String source = "/***** Start RHS source *****/\n" + this.rhs + "\n" + "/****** End RHS source ******/";
            String[] lines = source.split("\n");
            for (String line : lines) {
                target
                        .append(TAB)
                        .append(TAB)
                        .append(TAB)
                        .append(line)
                        .append("\n");
            }

            // end of the class
            target.append(TAB).append(TAB)
                    .append("}\n")
                    .append(TAB)
                    .append("}")
            ;
        }
    }

    public static class RuleCompiledSourcesImpl<S extends RuleLiteralData<R, C>, R extends Rule, C extends LiteralPredicate> implements RuleCompiledSources<S, R, C> {

        private final RuleSource<S, R, C> source;
        private final Collection<CompiledPredicate<C>> conditions;
        private final Consumer<RhsContext> rhs;
        private final String classJavaSource;

        public RuleCompiledSourcesImpl(Class<?> ruleClass, RuleSource<S, R, C> source, String classJavaSource) {
            this.source = source;
            this.classJavaSource = classJavaSource;

            Map<LiteralPredicate, ConditionSource<C>> compiledConditions = new IdentityHashMap<>();
            for (ConditionSource<C> conditionSource : source.conditionSources) {
                compiledConditions.put(conditionSource.source, conditionSource);
            }

            Collection<C> originalConditions = source.delegate.conditions();
            this.conditions = new ArrayList<>(originalConditions.size());

            for (C condition : originalConditions) {
                ConditionSource<C> compiled = compiledConditions.get(condition);
                if (compiled == null) {
                    throw new IllegalStateException("Condition not found or not compiled");
                } else {
                    assert compiled.source.equals(condition);
                    this.conditions.add(new CompiledPredicateImpl<>(compiled, ruleClass));
                }
            }

            // Define RHS if present
            if (source.delegate.rhs() == null) {
                this.rhs = null;
            } else {
                this.rhs = fromClass(ruleClass);
            }
        }

        @SuppressWarnings("unchecked")
        private static Consumer<RhsContext> fromClass(Class<?> ruleClass) {
            try {
                return (Consumer<RhsContext>) ruleClass.getDeclaredField(RHS_INSTANCE_VAR).get(null);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new IllegalStateException("RHS source provided but not compiled");
            }
        }

        public String getClassJavaSource() {
            return classJavaSource;
        }

        @NonNull
        @Override
        public S getSources() {
            return source.delegate;
        }

        @NonNull
        @Override
        public Collection<CompiledPredicate<C>> conditions() {
            return conditions;
        }

        @Override
        public Consumer<RhsContext> rhs() {
            return this.rhs;
        }
    }

    private static class CompiledPredicateImpl<C extends LiteralPredicate> implements CompiledPredicate<C> {
        private final ConditionSource<C> compiled;
        private final PredicateImpl<C> delegate;

        public CompiledPredicateImpl(ConditionSource<C> compiled, Class<?> ruleClass) {
            this.compiled = compiled;
            C source = compiled.source;
            this.delegate = new PredicateImpl<>(getHandle(ruleClass, compiled.handleName), compiled.resolvedFields, source);
        }


        @Override
        public LhsField.Array<String, TypeField> resolvedFields() {
            return compiled.resolvedFields;
        }

        @Override
        public ValuesPredicate getPredicate() {
            return delegate;
        }

        @Override
        public C getSource() {
            return compiled.source;
        }

        @Override
        public String toString() {
            return getSource().toString();
        }

        static MethodHandle getHandle(Class<?> compiledClass, String name) {
            try {
                return (MethodHandle) compiledClass.getDeclaredField(name).get(null);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new IllegalStateException("Handle not found", e);
            }
        }

        static class PredicateImpl<C extends LiteralPredicate> implements ValuesPredicate {
            private final MethodHandle handle;
            private final LhsField.Array<String, TypeField> resolvedFields;
            private final C source;

            PredicateImpl(MethodHandle handle, LhsField.Array<String, TypeField> resolvedFields, C source) {
                this.handle = handle;
                this.resolvedFields = resolvedFields;
                this.source = source;
            }

            // Two conditions are considered equal if they have the same Java source and the same signature
            private static boolean sameCondition(PredicateImpl<?> p1, PredicateImpl<?> p2) {
                if(Objects.equals(p1.source.getSource(), p2.source.getSource())) {
                    if(p1.resolvedFields.length() == p2.resolvedFields.length()) {
                        for(int i = 0; i < p1.resolvedFields.length(); i++) {
                            TypeField f1 = p1.resolvedFields.get(i).field();
                            TypeField f2 = p2.resolvedFields.get(i).field();
                            String name1 = f1.getName();
                            String name2 = f2.getName();
                            Class<?> valueType1 = f1.getValueType();
                            Class<?> valueType2 = f2.getValueType();
                            Class<?> declaringType1 = f1.getDeclaringType().getJavaClass();
                            Class<?> declaringType2 = f2.getDeclaringType().getJavaClass();
                            if(!Objects.equals(name1, name2)) {
                                return false;
                            }
                            if(!Objects.equals(valueType1, valueType2)) {
                                return false;
                            }
                            if(!Objects.equals(declaringType1, declaringType2)) {
                                return false;
                            }
                        }
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                PredicateImpl<?> predicate = (PredicateImpl<?>) o;
                return sameCondition(this, predicate);
            }

            @Override
            public int hashCode() {
                return source.getSource().hashCode();
            }

            @Override
            public String toString() {
                return source.toString();
            }

            @Override
            public boolean test(IntToValue values) {
                try {
                    return (boolean) handle.invokeExact(values);
                } catch (Throwable t) {
                    Object[] args = new Object[resolvedFields.length()];
                    for (int i = 0; i < args.length; i++) {
                        args[i] = values.apply(i);
                    }
                    throw new IllegalStateException("Evaluation exception at " + source + ", fields: " + resolvedFields + ", post-exception values:" + Arrays.toString(args), t);
                }
            }
        }
    }
}
