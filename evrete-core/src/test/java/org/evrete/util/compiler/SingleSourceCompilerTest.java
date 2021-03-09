package org.evrete.util.compiler;

import org.junit.jupiter.api.Test;

class SingleSourceCompilerTest {

    @Test
    void compile1() {
        String code = "" +
                "package test.pkg;\n" +
                "\n" +
                "public class \t\t" +
                "MyClass {\n" +
                "\n" +
                "    void run() {\n" +
                "    }\n" +
                "}";

        ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
        SingleSourceCompiler compiler = new SingleSourceCompiler();
        byte[] result = compiler.compileToBytes(code, parentClassLoader);
        CompiledClassLoader classLoader = new CompiledClassLoader(parentClassLoader);
        Class<?> clazz = classLoader.buildClass(result);
        assert clazz.getClassLoader() == classLoader;
        assert clazz.getPackage().getName().equals("test.pkg");
    }


}