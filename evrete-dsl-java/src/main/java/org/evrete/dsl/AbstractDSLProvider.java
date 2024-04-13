package org.evrete.dsl;

import org.evrete.api.Knowledge;
import org.evrete.api.spi.DSLKnowledgeProvider;
import org.evrete.dsl.annotation.EnvironmentListener;
import org.evrete.dsl.annotation.FieldDeclaration;
import org.evrete.dsl.annotation.PhaseListener;
import org.evrete.dsl.annotation.Rule;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

import static org.evrete.dsl.Utils.LOGGER;

abstract class AbstractDSLProvider implements DSLKnowledgeProvider, Constants {

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
            return new DSLKnowledge(knowledge, meta);
        }
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

    protected MethodHandles.Lookup defaultLookup() {
        return MethodHandles.publicLookup();
    }
}


