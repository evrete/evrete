package org.evrete.util.compiler;

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


        ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
        SourceCompiler compiler = new SourceCompiler();
        BytesClassLoader classLoader = new BytesClassLoader(parentClassLoader);
        Class<?> clazz = compiler.compile(code, classLoader);
        assert clazz.getClassLoader() == classLoader;
        assert clazz.getPackage().getName().equals("test.pkg");
    }


}