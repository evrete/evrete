package org.evrete.runtime.compiler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collections;

class SourceCompilerTest {
    RuntimeClassloader classloader;
    SourceCompiler compiler;

    @BeforeEach
    void init() {
        classloader = new RuntimeClassloader(Thread.currentThread().getContextClassLoader());
        compiler = new SourceCompiler(classloader);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void compile1(boolean initClass) throws CompilationException, ClassNotFoundException {

        Assertions.assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("test.pkg1.A1", initClass, classloader)
        );

        String source = "\n" +
                "package test.pkg1;\n" +
                "import org.evrete.api.FactHandle;\n" +
                "public   class   A1  { String s=FactHandle.class.getName();}";

        compiler.compile(Collections.singletonList(source));
        Class.forName("test.pkg1.A1", initClass, classloader);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void compile2(boolean initClass) throws CompilationException, ClassNotFoundException {

        Assertions.assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("test.pkg1.A", initClass, classloader)
        );

        Assertions.assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("test.pkg2.B", initClass, classloader)
        );

        String classA = "\n" +
                "package test.pkg1;\n" +
                "import org.evrete.api.FactHandle;\n" +
                "public class A { String s=FactHandle.class.getName();}";

        String classB = "\n" +
                "package test.pkg2;\n" +
                "import test.pkg1.A;\n" +
                "class B extends A {}";

        compiler.compile(Arrays.asList(classA, classB));
        Class<?> a = Class.forName("test.pkg1.A", initClass, classloader);
        Class<?> b = Class.forName("test.pkg2.B", initClass, classloader);
        assert b.getSuperclass().equals(a);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void compile3(boolean initClass) throws CompilationException, ClassNotFoundException {

        Assertions.assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("test.pkg1.A", initClass, classloader)
        );

        Assertions.assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("test.pkg2.B", initClass, classloader)
        );

        String classA = "\n" +
                "package test.pkg1;\n" +
                "import org.evrete.api.FactHandle;\n" +
                "public class A { String s=FactHandle.class.getName();}";

        String classB = "\n" +
                "package test.pkg2;\n" +
                "import test.pkg1.A;\n" +
                "class B extends A {}";

        compiler.compile(Arrays.asList(classB, classA));
        Class<?> a = Class.forName("test.pkg1.A", initClass, classloader);
        Class<?> b = Class.forName("test.pkg2.B", initClass, classloader);
        assert b.getSuperclass().equals(a);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void compile4(boolean initClass) throws CompilationException, ClassNotFoundException {

        Assertions.assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("test.pkg1.A", initClass, classloader)
        );

        Assertions.assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("test.pkg2.B", initClass, classloader)
        );

        String classA = "\n" +
                "package test.pkg1;\n" +
                "import org.evrete.api.FactHandle;\n" +
                "public class A { String s=FactHandle.class.getName();}";


        compiler.compile(Collections.singletonList(classA));

        String classB = "package test.pkg2;\n" +
                "import test.pkg1.A;\n" +
                "class B extends A {}";

        compiler.compile(Collections.singletonList(classB));

        Class<?> a = Class.forName("test.pkg1.A", initClass, classloader);
        Class<?> b = Class.forName("test.pkg2.B", initClass, classloader);
        assert b.getSuperclass().equals(a);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void compile5(boolean initClass) throws CompilationException, ClassNotFoundException {

        Assertions.assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("test.pkg1.A", initClass, classloader)
        );

        Assertions.assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("test.pkg2.B", initClass, classloader)
        );

        Assertions.assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("test.pkg3.C", initClass, classloader)
        );

        String classA = "\n" +
                "package test.pkg1;\n" +
                "import org.evrete.api.FactHandle;\n" +
                "public class A { String s=FactHandle.class.getName();}";


        String classB = "\n" +
                "package test.pkg2;\n" +
                "import test.pkg1.A;\n" +
                "public class B extends A {}";

        compiler.compile(Arrays.asList(classB, classA));


        //
        String classC = "\n" +
                "package test.pkg3;\n" +
                "import test.pkg2.B;\n" +
                "class C extends B {}";

        compiler.compile(Collections.singletonList(classC));


        Class<?> a = Class.forName("test.pkg1.A", initClass, classloader);
        Class<?> b = Class.forName("test.pkg2.B", initClass, classloader);
        Class<?> c = Class.forName("test.pkg3.C", initClass, classloader);
        assert b.getSuperclass().equals(a);
        assert c.getSuperclass().equals(b);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void compileNested1(boolean initClass) throws CompilationException, ClassNotFoundException {

        Assertions.assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("test.pkg1.A", initClass, classloader)
        );

        String sourceA = "\n" +
                "package test.pkg1;\n" +
                "import org.evrete.api.FactHandle;\n" +
                "public class A {\n" +
                "    String s1 = FactHandle.class.getName();\n" +
                "    public static class Nested {\n" +
                "        String s2 = FactHandle.class.getName();\n" +
                "    }\n" +
                "}";

        compiler.compile(Collections.singletonList(sourceA));
        Class<?> a = Class.forName("test.pkg1.A", initClass, classloader);
        Class<?> nested = Class.forName("test.pkg1.A$Nested", initClass, classloader);
        assert nested.getEnclosingClass().equals(a);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void compileNested2(boolean initClass) throws CompilationException, ClassNotFoundException {

        Assertions.assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("test.pkg1.A", initClass, classloader)
        );

        String sourceA = "\n" +
                "package test.pkg1;\n" +
                "import org.evrete.api.FactHandle;\n" +
                "public class A {\n" +
                "    String s1 = FactHandle.class.getName();\n" +
                "    public static class Nested {\n" +
                "        String s2 = FactHandle.class.getName();\n" +
                "    }\n" +
                "}";

        String sourceB = "\n" +
                "package test.pkg2;\n" +
                "import test.pkg1.A;\n" +
                "public class B extends A.Nested {}";

        compiler.compile(Arrays.asList(sourceA, sourceB));
        Class<?> a = Class.forName("test.pkg1.A", initClass, classloader);
        Class<?> nested = Class.forName("test.pkg1.A$Nested", initClass, classloader);
        assert nested.getEnclosingClass().equals(a);
    }
}