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
        ServiceClassLoader classLoader = new ServiceClassLoader(parentClassLoader, service.getSecurity().getProtectionDomain(RuleScope.BOTH));
        try {
            Class<?> clazz = compiler.compile(code, classLoader);
            assert clazz.getClassLoader() == classLoader;
        } catch (CompilationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void compileMultiple() throws Exception {
        String classA = "\n" +
                "package test.pkg1;\n" +
                "import org.evrete.api.RuleScope;\n" +
                "public class A { String s=RuleScope.class.getName();}";

        String classB = "\n" +
                "package test.pkg2;\n" +
                "import test.pkg1.A;\n" +
                "class B extends A {}";


        KnowledgeService service = new KnowledgeService();
        ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
        SourceCompiler compiler = new SourceCompiler();
        ServiceClassLoader classLoader = new ServiceClassLoader(parentClassLoader, service.getSecurity().getProtectionDomain(RuleScope.BOTH));


        Class<?> clazzA = compiler.compile(classA, classLoader);
        Class<?> clazzB = compiler.compile(classB, classLoader);

        assert clazzA.isAssignableFrom(clazzB);
    }
}