package org.evrete.classes;

@SuppressWarnings("unused")
public class TypeA extends Base {
    private String str;

    private TypeD typeD;

    public TypeA(String id) {
        super(id);
        this.str = "";
    }

    public TypeA() {
        super(null);
        this.str = "";
    }

    public TypeA(int i) {
        super(i);
    }

    public TypeD getTypeD() {
        return typeD;
    }

    public void setTypeD(TypeD typeD) {
        this.typeD = typeD;
    }


    public String getStr() {
        return str;
    }

    public TypeA setStr(String s) {
        this.str = s;
        return this;
    }

    @Override
    public String toString() {
        return "TypeA{" +
                "i=" + i +
                '}';
    }
}
