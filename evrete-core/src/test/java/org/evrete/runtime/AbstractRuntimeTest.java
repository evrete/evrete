package org.evrete.runtime;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RuleCompiledSources;
import org.evrete.api.annotations.RuleElement;
import org.evrete.spi.minimal.DefaultLiteralSourceCompiler;
import org.evrete.util.CompilationException;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.evrete.Configuration.RULE_BASE_CLASS;

public class AbstractRuntimeTest {

    @Test
    void compileRuleset() throws CompilationException {

        KnowledgeService service = new KnowledgeService();
        KnowledgeRuntime knowledge = (KnowledgeRuntime) service.newKnowledge();

        DefaultRuleSetBuilder<Knowledge> builder = (DefaultRuleSetBuilder<Knowledge>) knowledge.builder();
        builder
                .newRule()
                .set(RULE_BASE_CLASS, BaseInteger.class.getCanonicalName())
                .forEach("$i", TypeInteger.class)
                .where("$i.value < 100")
                .where("$i.positive()")
                .execute(ctx -> {
                })
                .newRule()
                .set(RULE_BASE_CLASS, BaseDouble.class.getCanonicalName())
                .forEach("$d", TypeDouble.class)
                .where("$d.value < 100")
                .where("$d.positive()")
                .execute(ctx -> {
                });

        Collection<RuleCompiledSources<DefaultRuleLiteralData, DefaultRuleBuilder<?>>> sources;
        sources = knowledge.compileRuleset(builder);
        assert sources.size() == 2;
        for (RuleCompiledSources<DefaultRuleLiteralData, DefaultRuleBuilder<?>> source : sources) {
            DefaultLiteralSourceCompiler.RuleCompiledSourcesImpl<DefaultRuleLiteralData, DefaultRuleBuilder<?>> impl = (DefaultLiteralSourceCompiler.RuleCompiledSourcesImpl<DefaultRuleLiteralData, DefaultRuleBuilder<?>>) source;

            String javaSource = impl.getClassJavaSource();

            if (javaSource.contains(TypeInteger.class.getCanonicalName())) {
                assert javaSource.contains(BaseInteger.class.getCanonicalName());
            } else if (javaSource.contains(TypeDouble.class.getCanonicalName())) {
                assert javaSource.contains(BaseDouble.class.getCanonicalName());
            } else {
                throw new IllegalStateException();
            }
        }
    }

    public static class TypeInteger {
        public int value;

        @RuleElement
        public boolean positive() {
            return value > 0;
        }
    }


    public static class TypeDouble {
        public double value;

        @RuleElement
        public boolean positive() {
            return value > 0;
        }
    }

    public static class BaseInteger {

    }

    public static class BaseDouble {

    }
}
