package org.evrete.util.compiler;

import org.evrete.KnowledgeService;
import org.evrete.api.RuleScope;
import org.junit.jupiter.api.Test;

class SingleSourceCompilerTest {

    @Test
    void compile1() {
        String code = "\n" +
                "package test.pkg;\n" +
                "\n" +
                "/* comment 1 */ /* comment 2 */" +

                "class \t\t " +
                "MyClass {\n" +
                "\n" +
                "    void run() {\n" +
                "    String s = \"Hello\";\n" +
                "    }\n" +
                "}";


        KnowledgeService service = new KnowledgeService();
        ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
        SourceCompiler compiler = new SourceCompiler();
        BytesClassLoader classLoader = new BytesClassLoader(parentClassLoader, service.getSecurity().getProtectionDomain(RuleScope.BOTH));
        try {
            Class<?> clazz = compiler.compile(code, classLoader);
            assert clazz.getClassLoader() == classLoader;
        } catch (CompilationException e) {
            throw new IllegalStateException(e);
        }
    }
}