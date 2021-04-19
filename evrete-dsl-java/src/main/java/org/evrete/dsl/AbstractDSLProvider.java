package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.api.spi.DSLKnowledgeProvider;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.RuleSet;
import org.evrete.dsl.annotation.*;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static org.evrete.dsl.DSLSourceProvider.LOGGER;

abstract class AbstractDSLProvider implements DSLKnowledgeProvider {
    static final String PROVIDER_JAVA_S = "JAVA-SOURCE";
    static final String PROVIDER_JAVA_C = "JAVA-CLASS";
    static final String PROVIDER_JAVA_J = "JAVA-JAR";

    static Knowledge processRuleSet(Knowledge knowledge, Class<?> javaClass) {
        // 0. locate and warn about annotated non-public methods
        for (Method m : Utils.allNonPublicAnnotated(javaClass)) {
            LOGGER.warning("Method " + m + " declared in " + m.getDeclaringClass() + " is not public and will be disregarded.");
        }


        // 1. Scanning all the class methods and saving those with annotations
        MethodHandles.Lookup lookup = MethodHandles.lookup().in(javaClass);
        TypeResolver typeResolver = knowledge.getTypeResolver();
        List<RuleMethod> ruleMethods = new ArrayList<>();
        Collection<ListenerMethod> listenerMethods = new ArrayList<>();
        Collection<PredicateMethod> predicateMethods = new ArrayList<>();

        for (Method m : javaClass.getMethods()) {
            Rule ruleAnnotation = m.getAnnotation(Rule.class);
            PhaseListener listenerAnnotation = m.getAnnotation(PhaseListener.class);
            FieldDeclaration fieldAnnotation = m.getAnnotation(FieldDeclaration.class);

            if (ruleAnnotation != null) {
                ruleMethods.add(new RuleMethod(lookup, m, typeResolver));
            } else if (listenerAnnotation != null) {
                ListenerMethod lm = new ListenerMethod(lookup, m);
                if (lm.phases.isEmpty()) {
                    LOGGER.warning("Listener method " + m + " is not bound to any phases and will be disregarded.");
                } else {
                    listenerMethods.add(lm);
                }
            } else if (fieldAnnotation != null) {
                // Field declarations must be applied before any rules
                applyFieldDeclaration(knowledge, lookup, m, fieldAnnotation);
            }
        }

        // 2. Creating rules
        if (ruleMethods.isEmpty()) {
            LOGGER.warning("No rule methods found in the source, ruleset is empty");
        } else {
            // How to sort rules when no salience is specified
            RuleSet.Sort defaultSort = Utils.deriveSort(javaClass);
            ruleMethods.sort(new RuleComparator(defaultSort));

            // Rule builders
            for (RuleMethod rm : ruleMethods) {
                RuleBuilder<?> builder = knowledge.newRule(rm.getRuleName());
                builder.setSalience(rm.getSalience());
                LhsBuilder<?> lhsBuilder = builder.forEach(rm.getLhsParameters());
                Where predicates = rm.getPredicates();
                if (predicates != null) {
                    // String predicates
                    for (String s : predicates.value()) {
                        lhsBuilder.where(s);
                    }

                    // Method predicates
                    for (MethodPredicate mp : predicates.asMethods()) {
                        String methodName = mp.method();
                        String[] descriptor = mp.descriptor();
                        // We need method arg types for lookup
                        Class<?>[] signature = new Class<?>[descriptor.length];
                        FieldReference[] references = new FieldReference[descriptor.length];
                        for (int i = 0; i < descriptor.length; i++) {
                            FieldReference ref = lhsBuilder.resolveField(descriptor[i]);
                            references[i] = ref;
                            signature[i] = ref.field().getValueType();
                        }

                        MethodType methodType = MethodType.methodType(boolean.class, signature);
                        MethodWithValues method = RuleMethod.lookup(lookup, javaClass, methodName, methodType);
                        PredicateMethod predicate = PredicateMethod.factory(method);
                        predicateMethods.add(predicate);
                        lhsBuilder.where(predicate, references);
                    }

                }
                // RHS
                lhsBuilder.execute(rm);
            }
        }
        return new DSLKnowledge(knowledge, javaClass, ruleMethods, predicateMethods, listenerMethods);
    }

    @SuppressWarnings("unchecked")
    private static <T, V> void applyFieldDeclaration(Knowledge context, MethodHandles.Lookup lookup, Method method, FieldDeclaration declaration) {
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new MalformedResourceException("Non-static method " + method + " in the " + method.getDeclaringClass() + " is annotated as a field declaration");
        }

        Class<V> returnType = (Class<V>) method.getReturnType();
        if (returnType.equals(void.class) || returnType.equals(Void.class)) {
            throw new MalformedResourceException("Method " + method + " in the " + method.getDeclaringClass() + " is annotated as field declaration but is void");
        }

        Parameter[] parameters = method.getParameters();
        if (parameters.length != 1) {
            throw new MalformedResourceException("Method " + method + " in the " + method.getDeclaringClass() + " is annotated as field declaration but has zero or more than one parameters");
        }

        Class<T> factJavaType = (Class<T>) parameters[0].getType();
        String fieldName;

        if (declaration.name().isEmpty()) {
            fieldName = method.getName();
        } else {
            fieldName = declaration.name();
        }

        final MethodHandle handle;
        try {
            handle = lookup.unreflect(method);
        } catch (IllegalAccessException e) {
            throw new MalformedResourceException("Unable to access field declaration method", e);
        }

        context
                .getTypeResolver()
                .getOrDeclare(factJavaType)
                .declareField(fieldName, returnType, new Function<T, V>() {
                    @Override
                    public V apply(T t) {
                        try {
                            return (V) handle.invoke(t);
                        } catch (RuntimeException e1) {
                            throw e1;
                        } catch (Throwable e2) {
                            throw new RuntimeException(e2);
                        }
                    }
                });
    }


    static String[] toSourceString(Reader[] readers) throws IOException {
        String[] sources = new String[readers.length];
        for (int i = 0; i < readers.length; i++) {
            Reader reader = readers[i];
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line;
            StringBuilder source = new StringBuilder(4096);
            while ((line = bufferedReader.readLine()) != null) {
                source.append(line).append("\n");
            }
            bufferedReader.close();
            sources[i] = source.toString();
        }
        return sources;
    }

    static String[] toSourceString(Charset charset, InputStream... streams) throws IOException {
        String[] sources = new String[streams.length];
        for (int i = 0; i < sources.length; i++) {
            sources[i] = new String(toByteArray(streams[i]), charset);
        }
        return sources;
    }

    static byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            bos.write(buffer, 0, length);
        }
        bos.close();
        return bos.toByteArray();
    }

    @Override
    public final Knowledge create(KnowledgeService service, URL... resources) throws IOException {
        if (resources == null || resources.length == 0) throw new IOException("Empty resources");
        InputStream[] streams = new InputStream[resources.length];
        for (int i = 0; i < resources.length; i++) {
            streams[i] = resources[i].openStream();
        }
        Knowledge knowledge = create(service, streams);

        for (InputStream stream : streams) {
            stream.close();
        }
        return knowledge;
    }
}
