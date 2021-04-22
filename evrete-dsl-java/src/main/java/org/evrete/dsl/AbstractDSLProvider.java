package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.spi.DSLKnowledgeProvider;
import org.evrete.dsl.annotation.FieldDeclaration;
import org.evrete.dsl.annotation.PhaseListener;
import org.evrete.dsl.annotation.Rule;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;

import static org.evrete.dsl.Utils.LOGGER;

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
        RulesetMeta meta = new RulesetMeta(javaClass);
        for (Method m : javaClass.getMethods()) {
            Rule ruleAnnotation = m.getAnnotation(Rule.class);
            PhaseListener listenerAnnotation = m.getAnnotation(PhaseListener.class);
            FieldDeclaration fieldAnnotation = m.getAnnotation(FieldDeclaration.class);

            if (ruleAnnotation != null) {
                meta.addRuleMethod(m);
            } else if (listenerAnnotation != null) {
                meta.addListener(m);
            } else if (fieldAnnotation != null) {
                meta.addFieldDeclaration(m);
            }
        }

        if (meta.ruleMethods.isEmpty()) {
            LOGGER.warning("No rule methods found in the source, ruleset is empty");
            return knowledge;
        } else {
            return new DSLKnowledge(knowledge, meta);
        }

/*
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
                    //predicateMethods.add(predicate);
                    lhsBuilder.where(predicate, references);
                }

            }
            // RHS
            lhsBuilder.execute(rm);
        }
*/
        //}
        //return new DSLKnowledge(knowledge, meta);
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
