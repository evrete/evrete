package org.evrete.dsl;

import org.evrete.Configuration;
import org.evrete.api.Knowledge;
import org.evrete.api.RuntimeContext;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.builders.RuleSetBuilder;
import org.evrete.api.spi.DSLKnowledgeProvider;
import org.evrete.dsl.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.evrete.dsl.Utils.LOGGER;

abstract class AbstractDSLProvider implements DSLKnowledgeProvider, Constants {

    static final Class<?> TYPE_URL = URL.class;
    static final Class<?> TYPE_CHAR_SEQUENCE = CharSequence.class;
    static final Class<?> TYPE_READER = Reader.class;
    static final Class<?> TYPE_INPUT_STREAM = InputStream.class;
    static final Class<?> TYPE_CLASS = Class.class;

    static final String CHARSET_PROPERTY = "org.evrete.source-charset";
    static final String CHARSET_DEFAULT = "UTF-8";

/*
    static Knowledge processRuleSet(Knowledge knowledge, MethodHandles.Lookup lookup, Class<?> javaClass) {
        // 0. locate and warn about annotated non-public methods
        for (Method m : Utils.allNonPublicAnnotated(javaClass)) {
            LOGGER.warning("Method " + m + " declared in " + m.getDeclaringClass() + " is not public and will be disregarded.");
        }

        // 1. Scanning all the class methods and saving those with annotations
        RulesetMeta meta = new RulesetMeta(lookup, javaClass);
        for (Method m : javaClass.getMethods()) {
            Rule ruleAnnotation = m.getAnnotation(Rule.class);
            PhaseListener phaseListener = m.getAnnotation(PhaseListener.class);
            FieldDeclaration fieldDeclaration = m.getAnnotation(FieldDeclaration.class);
            EnvironmentListener envListener = m.getAnnotation(EnvironmentListener.class);

            if (ruleAnnotation != null) {
                meta.addRuleMethod(m);
            }

            if (phaseListener != null) {
                meta.addPhaseListener(m);
            }

            if (fieldDeclaration != null) {
                meta.addFieldDeclaration(m, fieldDeclaration.type());
            }

            if (envListener != null) {
                String property = envListener.value();
                if (property.isEmpty()) {
                    LOGGER.warning("The @" + EnvironmentListener.class.getSimpleName() + " annotation on " + m + " has no property value and will be ignored");
                } else {
                    meta.addEnvListener(m, property);
                }
            }
        }

        if (meta.ruleMethods.isEmpty()) {
            return knowledge;
        } else {
            return new DSLKnowledge(knowledge, null, meta);
        }
    }
*/


    <C extends RuntimeContext<C>> Stream<Class<?>> sourceClasses(RuntimeContext<C> context, URL[] urls) throws IOException {
        throw new IllegalArgumentException("URL sources are not supported by this provider (" + this.getClass().getName() + ")");
    }

    <C extends RuntimeContext<C>> Stream<Class<?>> sourceClasses(RuntimeContext<C> context, Reader[] readers) throws IOException {
        throw new IllegalArgumentException("Reader sources are not supported by this provider (" + this.getClass().getName() + ")");
    }

    <C extends RuntimeContext<C>> Stream<Class<?>> sourceClasses(RuntimeContext<C> context, CharSequence[] strings) throws IOException {
        throw new IllegalArgumentException("CharSequence(String) sources are not supported by this provider (" + this.getClass().getName() + ")");
    }

    <C extends RuntimeContext<C>> Stream<Class<?>> sourceClasses(RuntimeContext<C> context, InputStream[] inputStreams) throws IOException {
        throw new IllegalArgumentException("Input stream sources are not supported by this provider (" + this.getClass().getName() + ")");
    }

    <C extends RuntimeContext<C>> Stream<Class<?>> sourceClasses(RuntimeContext<C> context, Class<?>[] classes) throws IOException {
        throw new IllegalArgumentException("Java Class sources are not supported by this provider (" + this.getClass().getName() + ")");
    }

    private <C extends RuntimeContext<C>, S> Stream<Class<?>> readClassesFrom(RuntimeContext<C> context, @NonNull S[] sources) {

        Class<?> componentType = sources.getClass().getComponentType();
        try {
            if (TYPE_URL.isAssignableFrom(componentType)) {
                return sourceClasses(context, (URL[]) sources);
            } else if (TYPE_READER.isAssignableFrom(componentType)) {
                return sourceClasses(context, (Reader[]) sources);
            } else if (TYPE_CHAR_SEQUENCE.isAssignableFrom(componentType)) {
                return sourceClasses(context, (CharSequence[]) sources);
            } else if (TYPE_INPUT_STREAM.isAssignableFrom(componentType)) {
                return sourceClasses(context, (InputStream[]) sources);
            } else if (TYPE_CLASS.isAssignableFrom(componentType)) {
                return sourceClasses(context, (Class<?>[]) sources);
            } else {
                throw new IllegalArgumentException("Unsupported source type " + componentType);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public <C extends RuntimeContext<C>, S> void appendTo(RuleSetBuilder<C> target, Predicate<String> nameFilter, S[] sources) throws IOException {
        if (sources != null && sources.length > 0) {
            try {
                this.appendToInner(target, nameFilter, sources);
            } catch (UncheckedIOException e) {
                // Unwrap stream exceptions
                throw e.getCause();
            }
        } else  {
            LOGGER.warning(()->"No sources specified");
        }
    }

    private <C extends RuntimeContext<C>, S> void appendToInner(RuleSetBuilder<C> target, Predicate<String> nameFilter, S[] sources) {
        Optional<Class<?>[]> sourceTypes = sourceTypes();
        if (sourceTypes.isPresent()) {
            RuntimeContext<C> context = target.getContext();
            List<WrappedClass> classesWithRules = readClassesFrom(context, sources)
                    .filter(clazz -> {
                        // Applying the ruleset name filter
                        RuleSet annotation = clazz.getAnnotation(RuleSet.class);
                        String ruleSetName = annotation == null ? null : annotation.value();
                        return nameFilter.test(ruleSetName);
                    })
                    .filter(clazz -> {
                        // Filter out classes without the @Rule declarations
                        boolean hasRules = false;
                        for (Method method : clazz.getMethods()) {
                            if (method.getAnnotation(Rule.class) != null) {
                                hasRules = true;
                                break;
                            }
                        }
                        return hasRules;
                    })
                    .map(WrappedClass::new)
                    .collect(Collectors.toList());
            appendTo(target, WrappedClass.excludeSubclasses(classesWithRules));
        } else {
            throw new IllegalArgumentException("Source types not specified, please report the bug.");
        }
    }

    private <C extends RuntimeContext<C>> void appendTo(RuleSetBuilder<C> target, List<RulesClass> ruleClasses) {
        // 1. Configure types
        target.getContext().configureTypes(resolver ->{
            for(RulesClass rulesClass : ruleClasses) {
                for(RulesClass.FieldDeclarationMethod declarationMethod :  rulesClass.fieldDeclarationMethods.methods.getData()) {
                    declarationMethod.selfRegister(resolver);
                }
            }
        });
    }

    static DSLKnowledgeBuilder processRuleSet(RuleSetBuilder<Knowledge> rulesetBuilder, MethodHandles.Lookup lookup, Class<?> javaClass) {
        // 0. locate and warn about annotated non-public methods
        for (Method m : Utils.allNonPublicAnnotated(javaClass)) {
            LOGGER.warning(()->"Method " + m + " declared in " + m.getDeclaringClass() + " is not public and will be disregarded.");
        }

        // 1. Scanning all the class methods and saving those with annotations
        RulesetMeta meta = new RulesetMeta(lookup, javaClass);
        for (Method m : javaClass.getMethods()) {
            Rule ruleAnnotation = m.getAnnotation(Rule.class);
            PhaseListener phaseListener = m.getAnnotation(PhaseListener.class);
            FieldDeclaration fieldDeclaration = m.getAnnotation(FieldDeclaration.class);
            EnvironmentListener envListener = m.getAnnotation(EnvironmentListener.class);

            if (ruleAnnotation != null) {
                meta.addRuleMethod(m);
            }

            if (phaseListener != null) {
                meta.addPhaseListener(m);
            }

            if (fieldDeclaration != null) {
                meta.addFieldDeclaration(m, fieldDeclaration.type());
            }

            if (envListener != null) {
                String property = envListener.value();
                if (property.isEmpty()) {
                    LOGGER.warning(()->"The @" + EnvironmentListener.class.getSimpleName() + " annotation on " + m + " has no property value and will be ignored");
                } else {
                    meta.addEnvListener(m, property);
                }
            }
        }

        if (meta.ruleMethods.isEmpty()) {
            return null;
        } else {
            return new DSLKnowledgeBuilder(rulesetBuilder, meta);
        }
    }

    protected MethodHandles.Lookup defaultLookup() {
        return MethodHandles.publicLookup();
    }

    static Charset charset(Configuration configuration) {
        String charSet = configuration.getProperty(CHARSET_PROPERTY, CHARSET_DEFAULT);
        return Charset.forName(charSet);
    }
}


