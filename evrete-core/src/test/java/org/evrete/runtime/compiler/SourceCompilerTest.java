package org.evrete.runtime.compiler;

import org.evrete.api.JavaSourceCompiler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

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

        compiler.compile(Collections.singleton(source));
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

        compiler.compile(new HashSet<>(Arrays.asList(classA, classB)));
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

        // Use inverse order
        compiler.compile(new HashSet<>(Arrays.asList(classB, classA)));
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


        compiler.compile(Collections.singleton(classA));

        String classB = "package test.pkg2;\n" +
                "import test.pkg1.A;\n" +
                "class B extends A {}";

        compiler.compile(Collections.singleton(classB));

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

        compiler.compile(new HashSet<>(Arrays.asList(classB, classA)));


        //
        String classC = "\n" +
                "package test.pkg3;\n" +
                "import test.pkg2.B;\n" +
                "class C extends B {}";

        compiler.compile(Collections.singleton(classC));


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

        compiler.compile(Collections.singleton(sourceA));
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

        compiler.compile(new HashSet<>(Arrays.asList(sourceA, sourceB)));
        Class<?> a = Class.forName("test.pkg1.A", initClass, classloader);
        Class<?> nested = Class.forName("test.pkg1.A$Nested", initClass, classloader);
        assert nested.getEnclosingClass().equals(a);
    }

    @Test
    void compileMultipleShuffled() throws CompilationException {

        List<String> sources = new ArrayList<>();
        String root = "\n" +
                "package test.pkg;\n" +
                "public class A0 {}";

        sources.add(root);
        for (int i = 1; i < 30; i++) {
            String next = "\n" +
                    "package test.pkg;\n" +
                    "import test.pkg.A" + (i-1)+ ";\n" +
                    "public class A" + i +" extends A"+ (i-1) + "  {}";
            sources.add(next);
        }

        Collections.shuffle(sources);

        Map<String, Class<?>> compiled = compiler.compile(new HashSet<>(sources));
        assert compiled.size() == sources.size();

        compiled.entrySet().iterator().forEachRemaining(e -> {
            String source = e.getKey();
            Class<?> cl = e.getValue();
            assert source.contains(cl.getSimpleName());
        });
    }

    @Test
    void testException()  {
        List<JavaSourceCompiler.ClassSource> invalidSources = new LinkedList<>();

        for (int i = 0; i < 10; i++) {
            invalidSources.add(new InvalidSource(i));
        }

        try {
            compiler.compile(invalidSources);
        } catch (CompilationException e) {
            assert e.getOtherErrors().isEmpty();
            for(JavaSourceCompiler.ClassSource source : e.getErrorSources()) {
                assert source instanceof InvalidSource;
                assert e.getErrorMessage(source) != null;
            }
        }
    }

    static class InvalidSource implements JavaSourceCompiler.ClassSource {
        private final int idx;

        public InvalidSource(int idx) {
            this.idx = idx;
        }

        @Override
        public String binaryName() {
            return "test.pkg.Clazz" + idx;
        }

        @Override
        public String getSource() {
            return "Hello World";
        }
    }
}