package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.LhsBuilder;
import org.evrete.api.RuleBuilder;
import org.evrete.api.spi.DSLKnowledgeProvider;
import org.evrete.dsl.annotation.MethodPredicate;
import org.evrete.dsl.annotation.Where;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;

abstract class AbstractJavaDSLProvider implements DSLKnowledgeProvider {

    static final String PROVIDER_JAVA_S = "JAVA-SOURCE";
    static final String PROVIDER_JAVA_C = "JAVA-CLASS";
    static final String PROVIDER_JAVA_J = "JAVA-JAR";

    static JavaClassRuleSet processRuleSet(Knowledge knowledge, Class<?> javaClass) {
        // Build rules

        JavaClassRuleSet jcr = new JavaClassRuleSet(knowledge, javaClass);

        for (RuleMethod rm : jcr.getRuleMethods()) {
            RuleBuilder<?> builder = knowledge.newRule(rm.getName());
            builder.setSalience(rm.getSalience());
            // Build LHS from method parameters
            LhsParameter[] factParameters = rm.getLhsParameters();

            // Apply condition annotations
            // 1. String predicates
            LhsBuilder<?> lhsBuilder = builder.forEach(factParameters);
            Where predicates = rm.getPredicates();
            if (predicates != null) {
                // 1. String predicates
                for (String stringPredicate : predicates.value()) {
                    lhsBuilder = lhsBuilder.where(stringPredicate);
                }

                // 2. Method predicates
                for (MethodPredicate methodPredicate : predicates.asMethods()) {
                    lhsBuilder.where(jcr.resolve(lhsBuilder, rm, methodPredicate), methodPredicate.descriptor());
                }
            }
            // Final step - RHS
            lhsBuilder.execute(rm);
        }
        return jcr;
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

}
