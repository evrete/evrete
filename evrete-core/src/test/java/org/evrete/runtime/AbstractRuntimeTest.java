package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RuleCompiledSources;
import org.evrete.api.annotations.RuleElement;
import org.evrete.runtime.compiler.DefaultLiteralSourceCompiler;
import org.evrete.util.CompilationException;
import org.junit.jupiter.api.Assertions;
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

        Collection<RuleCompiledSources<DefaultRuleLiteralData, DefaultRuleBuilder<?>, DefaultConditionManager.Literal>> sources;
        sources = knowledge.compileRuleset(builder);
        assert sources.size() == 2;
        for (RuleCompiledSources<DefaultRuleLiteralData, DefaultRuleBuilder<?>, DefaultConditionManager.Literal> source : sources) {
            DefaultLiteralSourceCompiler.RuleCompiledSourcesImpl<DefaultRuleLiteralData, DefaultRuleBuilder<?>, DefaultConditionManager.Literal> impl = (DefaultLiteralSourceCompiler.RuleCompiledSourcesImpl<DefaultRuleLiteralData, DefaultRuleBuilder<?>, DefaultConditionManager.Literal>) source;

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

    @Test
    void testCompilationDisabledFlagOn1() {
        Configuration configuration = new Configuration();
        configuration.setProperty(Configuration.DISABLE_LITERAL_DATA, "tRue");
        KnowledgeService service = new KnowledgeService(configuration);

        Knowledge knowledge = service.newKnowledge();

        Assertions.assertThrows(
                IllegalStateException.class,
                () -> knowledge.builder()
                        .newRule()
                        .forEach("$i", TypeInteger.class)
                        .where("$i.value < 100")
                        .execute(ctx -> {
                        })
                        .build()
        );
    }

    @Test
    void testCompilationDisabledFlagOn2() {
        Configuration configuration = new Configuration();
        configuration.setProperty(Configuration.DISABLE_LITERAL_DATA, "TRUE");
        KnowledgeService service = new KnowledgeService(configuration);

        Knowledge knowledge = service.newKnowledge();

        Assertions.assertThrows(
                IllegalStateException.class,
                () -> knowledge.builder()
                        .newRule()
                        .forEach("$i", TypeInteger.class)
                        .where()
                        .execute("System.out.println()")
                        .build()
        );
    }

    @Test
    void testCompilationDisabledFlagOff1() {
        Configuration configuration = new Configuration();
        configuration.setProperty(Configuration.DISABLE_LITERAL_DATA, "False");
        KnowledgeService service = new KnowledgeService(configuration);

        Knowledge knowledge = service.newKnowledge();

        knowledge.builder()
                .newRule()
                .forEach("$i", TypeInteger.class)
                .where("$i.value < 100")
                .execute(ctx -> {
                })
                .build();
    }


    @Test
    void testCompilationDisabledFlagOff2() {
        Configuration configuration = new Configuration();
        configuration.setProperty(Configuration.DISABLE_LITERAL_DATA, "Something not 'true'");
        KnowledgeService service = new KnowledgeService(configuration);

        Knowledge knowledge = service.newKnowledge();

        knowledge.builder()
                .newRule()
                .forEach("$i", TypeInteger.class)
                .where("$i.value < 100")
                .execute(ctx -> {
                })
                .build();
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
