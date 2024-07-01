package org.evrete.spi.minimal.compiler;

class ClassMeta {
    private final String simpleName;
    private final String packageName;

    protected ClassMeta(String packageName, String simpleName) {
        this.simpleName = simpleName;
        this.packageName = packageName;
    }

    protected ClassMeta(String binaryName) {
        this(packageName(binaryName), simpleName(binaryName));
    }

    public String getBinaryName() {
        return packageName + "." + simpleName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    static String packageName(String binaryName) {
        int dotPos = binaryName.lastIndexOf('.');
        if (dotPos < 0) {
            throw new IllegalArgumentException("Empty/default packages are not supported");
        }
        return binaryName.substring(0, dotPos);
    }

    static String simpleName(String binaryName) {
        int dotPos = binaryName.lastIndexOf('.');
        if (dotPos < 0) {
            throw new IllegalArgumentException("Empty/default packages are not supported");
        }
        return binaryName.substring(dotPos + 1);
    }

}
