package org.evrete.util.compiler;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static javax.tools.StandardLocation.CLASS_PATH;
import static javax.tools.StandardLocation.SOURCE_PATH;

final class FileManager<M extends JavaFileManager> extends ForwardingJavaFileManager<M> {
    private final ByteArrayOutputStream bos;
    private final ServiceClassLoader classLoader;
    private final PackageExplorer finder;

    private FileManager(M fileManager, ServiceClassLoader classLoader) {
        super(fileManager);
        this.bos = new ByteArrayOutputStream();
        this.classLoader = classLoader;
        this.finder = new PackageExplorer(classLoader);
    }

    public static FileManager<?> instance(JavaCompiler compiler, ServiceClassLoader classLoader) {
        return new FileManager<>(
                compiler.getStandardFileManager(null, null, null),
                classLoader
        );
    }

    byte[] getBytes() {
        try {
            bos.close();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        JavaFileObject o = super.getJavaFileForOutput(location, className, kind, sibling);
        return new ClassBytesFileObject<>(o, bos);
    }

    @Override
    public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException {
        return super.getFileForOutput(location, packageName, relativeName, sibling);
    }

    @Override
    public ClassLoader getClassLoader(Location location) {
        return classLoader;
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
        if (location.isOutputLocation()) throw new IllegalStateException();
        if(recurse) {
            return super.list(location, packageName, kinds, true);
        }

        boolean searchScope = (location == SOURCE_PATH || location == CLASS_PATH) && !packageName.startsWith("java");
        if (searchScope) {
            List<JavaFileObject> found = finder.find(packageName);
            found.addAll(classLoader.getCompiledClasses(packageName));
            return found;
        } else {
            return super.list(location, packageName, kinds, false);
        }
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof AbstractCompiledClass) {
            return ((AbstractCompiledClass) file).getBinaryName();
        } else { // if it's not CustomJavaFileObject, then it's coming from standard file manager - let it handle the file
            return super.inferBinaryName(location, file);
        }
    }
}
