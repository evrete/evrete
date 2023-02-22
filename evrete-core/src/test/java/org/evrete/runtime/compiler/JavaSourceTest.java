
package org.evrete.runtime.compiler;

import org.junit.jupiter.api.Test;

class JavaSourceTest {

    @Test
    void parse1() {
        String source = "\n" +
                "package test.pkg1;\n" +
                "import org.evrete.api.FactHandle;\n" +
                "public   class   A1     { String s=FactHandle.class.getName();}";

        JavaSource src = JavaSource.parse(1, source);

        assert "A1".equals(src.getClassName());
    }
    @Test

    void parse2() {
        String source = "\n" +
                "package test.pkg1;\n" +
                "import org.evrete.api.FactHandle;\n" +
                "public   class   A1  extends B  { String s=FactHandle.class.getName();}";

        JavaSource src = JavaSource.parse(0, source);

        assert "A1".equals(src.getClassName());
    }
/**/
    @Test
    void removeBlockComments1() {
        String arg = "A/*Hello World*/";
        assert "A".equals(JavaSource.removeBlockComments(arg));
    }
    @Test
    void removeBlockComments2() {
        String arg = "/*Hello World*/";
        assert "".equals(JavaSource.removeBlockComments(arg));
    }
    @Test
    void removeBlockComments3() {
        String arg = "A/**/";
        assert "A".equals(JavaSource.removeBlockComments(arg));
    }

    @Test
    void removeBlockComments4() {
        String arg = "/**/";
        assert "".equals(JavaSource.removeBlockComments(arg));
    }

    /*//**/
    @Test
    void removeBlockComments5() {
        String arg = "/**//**//**//**/";
        assert "".equals(JavaSource.removeBlockComments(arg));
    }
    @Test
    void removeBlockComments6() {
        String arg = "A/* 0 *//* 1 *//* 2 *//* /*3 */B/* /*4 */";
        assert "AB".equals(JavaSource.removeBlockComments(arg));
    }

}