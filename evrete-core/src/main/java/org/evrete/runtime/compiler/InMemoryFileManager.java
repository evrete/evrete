package org.evrete.runtime.compiler;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static javax.tools.StandardLocation.CLASS_PATH;

final class InMemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
    private static final Logger LOGGER = Logger.getLogger(InMemoryFileManager.class.getName());
    private final RuntimeClassloader classLoader;
    private final PackageExplorer finder;

    private final Collection<DestinationClassObject> output = new LinkedList<>();
    private final Map<String, List<JavaSource>> sources = new HashMap<>();

    InMemoryFileManager(JavaFileManager fileManager, RuntimeClassloader classLoader, Collection<JavaSource> sources) {
        super(fileManager);
        this.classLoader = classLoader;
        this.finder = new PackageExplorer(classLoader);
        for (JavaSource source : sources) {
            this.sources.computeIfAbsent(source.getPackageName(), k -> new LinkedList<>()).add(source);
        }
    }

    public Collection<DestinationClassObject> getOutput() {
        return output;
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
        DestinationClassObject compiled = new DestinationClassObject(className);
        output.add(compiled);
        return compiled;
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


        Iterable<JavaFileObject> defaultFiles = super.list(location, packageName, kinds, recurse);
        if (location != CLASS_PATH || packageName.startsWith("java.") || packageName.equals("java")) {
            return defaultFiles;
        }

        Collection<JavaFileObject> result = new LinkedList<>();
        defaultFiles.iterator().forEachRemaining(result::add);
        result.addAll(classLoader.getDefinedClasses(packageName));
        result.addAll(finder.find(packageName));
        return result;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof AbstractJavaObject) {
            return ((AbstractJavaObject) file).getBinaryName();
        } else { // if it's not CustomJavaFileObject, then it's coming from standard file manager - let it handle the file
            return super.inferBinaryName(location, file);
        }
    }
}
