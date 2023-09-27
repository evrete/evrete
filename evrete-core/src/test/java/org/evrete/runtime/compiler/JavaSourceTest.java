
package org.evrete.runtime.compiler;

import org.evrete.api.JavaSourceCompiler;
import org.junit.jupiter.api.Test;

class JavaSourceTest {

    @Test
    void parse1() {
        String source = "\n" +
                "package test.pkg1;\n" +
                "import org.evrete.api.FactHandle;\n" +
                "public   class   A1     { String s=FactHandle.class.getName();}";

        JavaSourceCompiler.ClassSource src = JavaSourceObject.parse(source);

        assert "A1".equals(new ClassMeta(src.binaryName()).getSimpleName());
    }
    @Test

    void parse2() {
        String source = "\n" +
                "package test.pkg1;\n" +
                "import org.evrete.api.FactHandle;\n" +
                "public   class   A1  extends B  { String s=FactHandle.class.getName();}";

        JavaSourceCompiler.ClassSource src = JavaSourceObject.parse(source);

        assert "A1".equals(new ClassMeta(src.binaryName()).getSimpleName());
    }

    @Test
    void removeBlockComments1() {
        String arg = "A/*Hello World*/";
        assert "A".equals(JavaSourceObject.removeBlockComments(arg));
    }
    @Test
    void removeBlockComments2() {
        String arg = "/*Hello World*/";
        assert JavaSourceObject.removeBlockComments(arg).isEmpty();
    }
    @Test
    void removeBlockComments3() {
        String arg = "A/**/";
        assert "A".equals(JavaSourceObject.removeBlockComments(arg));
    }

    @Test
    void removeBlockComments4() {
        String arg = "/**/";
        assert JavaSourceObject.removeBlockComments(arg).isEmpty();
    }

    /*//**/
    @Test
    void removeBlockComments5() {
        String arg = "/**//**//**//**/";
        assert JavaSourceObject.removeBlockComments(arg).isEmpty();
    }
    @Test
    void removeBlockComments6() {
        String arg = "A/* 0 *//* 1 *//* 2 *//* /*3 */B/* /*4 */";
        assert "AB".equals(JavaSourceObject.removeBlockComments(arg));
    }

}