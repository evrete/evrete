package org.evrete.dsl;

import org.evrete.api.FactBuilder;
import org.evrete.api.RuleBuilder;
import org.evrete.api.RuntimeContext;
import org.evrete.dsl.annotation.MethodPredicate;
import org.evrete.dsl.annotation.Where;
import org.evrete.runtime.builder.LhsBuilder;

import java.io.*;
import java.nio.charset.Charset;

class AbstractJavaDSLProvider {

    static void processRuleSet(RuntimeContext<?> targetContext, JavaClassRuleSet ruleSet) {
        // Build rules
        for (RuleMethod rm : ruleSet.getRuleMethods()) {
            RuleBuilder<?> builder = targetContext.newRule(rm.getName());
            builder.setSalience(rm.getSalience());
            // Build LHS from method parameters
            LhsParameter[] factParameters = rm.getLhsParameters();
            FactBuilder[] facts = new FactBuilder[factParameters.length];

            for (int i = 0; i < factParameters.length; i++) {
                LhsParameter lhsParameter = factParameters[i];
                facts[i] = FactBuilder.fact(lhsParameter.getLhsRef(), lhsParameter.getFactType());
            }

            // Apply condition annotations
            // 1. String predicates
            LhsBuilder<?> lhsBuilder = builder.forEach(facts);
            Where predicates = rm.getPredicates();
            if (predicates != null) {
                for (String stringPredicate : predicates.asStrings()) {
                    lhsBuilder = lhsBuilder.where(stringPredicate);
                }

                // 2. Method predicates
                for (MethodPredicate methodPredicate : predicates.asMethods()) {
                    lhsBuilder.where(ruleSet.resolve(lhsBuilder, rm, methodPredicate), methodPredicate.descriptor());
                }
            }
            // Final step - RHS
            lhsBuilder.execute(rm);
        }
    }

    static String toSourceString(Reader reader) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line;
        StringBuilder source = new StringBuilder(4096);
        while ((line = bufferedReader.readLine()) != null) {
            source.append(line).append("\n");
        }
        bufferedReader.close();
        return source.toString();
    }

    static String toSourceString(Charset charset, InputStream is) throws IOException {
        return new String(toByteArray(is), charset);
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
