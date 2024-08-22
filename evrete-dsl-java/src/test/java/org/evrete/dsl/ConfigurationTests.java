package org.evrete.dsl;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.annotations.RuleElement;
import org.evrete.dsl.annotation.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class ConfigurationTests {

    @Test
    void testCompilationDisabledFlagOn1() {
        Configuration configuration = new Configuration();
        configuration.set(Configuration.DISABLE_LITERAL_DATA, "true");
        KnowledgeService service = new KnowledgeService(configuration);
        Assertions.assertThrows(
                IllegalStateException.class,
                () -> service
                .newKnowledge()
                .importRules(
                        Constants.PROVIDER_JAVA_CLASS,
                        RulesetWithLiterals.class
                )
        );
    }

    @Test
    void testCompilationDisabledFlagOn2() throws IOException {
        Configuration configuration = new Configuration();
        configuration.set(Configuration.DISABLE_LITERAL_DATA, "true");
        KnowledgeService service = new KnowledgeService(configuration);
        service
                .newKnowledge()
                .importRules(
                        Constants.PROVIDER_JAVA_CLASS,
                        RulesetWithoutLiterals1.class
                );
    }

    @Test
    void testCompilationDisabledFlagOn3() throws IOException {
        Configuration configuration = new Configuration();
        configuration.set(Configuration.DISABLE_LITERAL_DATA, "true");
        KnowledgeService service = new KnowledgeService(configuration);
        service
                .newKnowledge()
                .importRules(
                        Constants.PROVIDER_JAVA_CLASS,
                        RulesetWithoutLiterals2.class
                );
    }

    @Test
    void testCompilationDisabledFlagOff() throws IOException {
        Configuration configuration = new Configuration();
        configuration.set(Configuration.DISABLE_LITERAL_DATA, "not true");
        KnowledgeService service = new KnowledgeService(configuration);
        service
                .newKnowledge()
                .importRules(
                        Constants.PROVIDER_JAVA_CLASS,
                        RulesetWithLiterals.class
                );
    }

    @RuleSet
    public static class RulesetWithLiterals {

        @Rule
        @Where("$i > 0")
        public void rule1(@Fact("$i") Integer i) {

        }
    }

    @RuleSet
    public static class RulesetWithoutLiterals1 {

        @Rule
        @Where("")
        public void rule1(@Fact("$i") Integer i) {

        }
    }

    @RuleSet
    public static class RulesetWithoutLiterals2 {

        @Rule
        @Where(methods = {
                @MethodPredicate(method = "conditionMethod", args = {"$i"})
        })
        public void rule1(@Fact("$i") Integer i) {

        }

        @RuleElement
        public boolean conditionMethod(Integer i) {
            return i > 0;
        }
    }
}
