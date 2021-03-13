package org.evrete.dsl;

import org.evrete.api.FactBuilder;
import org.evrete.api.RuleBuilder;
import org.evrete.api.RuntimeContext;
import org.evrete.api.spi.DSLKnowledgeProvider;
import org.evrete.dsl.annotation.MethodPredicate;
import org.evrete.dsl.annotation.Where;
import org.evrete.runtime.builder.LhsBuilder;
import org.evrete.util.compiler.CompiledClassLoader;
import org.evrete.util.compiler.SingleSourceCompiler;

import java.io.*;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class JavaSourceDSLProvider implements DSLKnowledgeProvider {
    static final Logger LOGGER = Logger.getLogger(JavaSourceDSLProvider.class.getName());
    static final String NAME = "JAVA-SOURCE";
    private static final String CHARSET_PROPERTY = "org.evrete.source-charset";
    private static final String CHARSET_DEFAULT = StandardCharsets.UTF_8.name();

    private static void apply(RuntimeContext<?> targetContext, String source) {
        ClassLoader ctxClassLoader = targetContext.getClassLoader();
        CompiledClassLoader extendedClassLoader = new CompiledClassLoader(ctxClassLoader);
        SingleSourceCompiler compiler = new SingleSourceCompiler();
        Class<?> ruleSet = compiler.compile(source, extendedClassLoader);

        if (Modifier.isPublic(ruleSet.getModifiers())) {
            try {
                Object ruleSetInstance = ruleSet.getConstructor().newInstance();
                JavaClassRuleSet crs = new JavaClassRuleSet(ruleSet, ruleSetInstance);
                processRuleSet(targetContext, crs);
            } catch (MalformedResourceException t) {
                throw t;
            } catch (Throwable t) {
                throw new MalformedResourceException("Unable to create class instance", t);
            }
        } else {
            throw new MalformedResourceException("Source must be a public class");
        }
    }

    private static void processRuleSet(RuntimeContext<?> targetContext, JavaClassRuleSet ruleSet) {
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

    private static String toSourceString(Reader reader) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line;
        StringBuilder source = new StringBuilder(4096);
        while ((line = bufferedReader.readLine()) != null) {
            source.append(line).append("\n");
        }
        bufferedReader.close();
        return source.toString();
    }

    private static String toSourceString(Charset charset, InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            bos.write(buffer, 0, length);
        }
        bos.close();
        return new String(bos.toByteArray(), charset);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void apply(RuntimeContext<?> targetContext, InputStream inputStream) throws IOException {
        String charSet = targetContext.getConfiguration().getProperty(CHARSET_PROPERTY, CHARSET_DEFAULT);
        String source = toSourceString(Charset.forName(charSet), inputStream);
        apply(targetContext, source);
    }

    @Override
    public void apply(RuntimeContext<?> targetContext, Reader reader) throws IOException {
        String source = toSourceString(reader);
        apply(targetContext, source);
    }

}
