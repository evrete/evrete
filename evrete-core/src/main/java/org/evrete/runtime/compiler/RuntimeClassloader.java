package org.evrete.runtime.compiler;

import javax.tools.JavaFileObject;
import java.security.SecureClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.evrete.runtime.compiler.SourceCompiler.packageName;

public class RuntimeClassloader extends SecureClassLoader {

    private static final AtomicLong instanceCounter = new AtomicLong();
    private static final Collection<JavaFileObject> EMPTY = Collections.emptyList();
    private final Map<String, Collection<String>> definedClasses = new ConcurrentHashMap<>();

    private final Map<String, byte[]> classDefinitions = new ConcurrentHashMap<>();

    private final long instanceId;

    public RuntimeClassloader(ClassLoader parent) {
        super(parent);
        this.instanceId = instanceCounter.incrementAndGet();
    }

    long getInstanceId() {
        return instanceId;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        synchronized (this) {
            byte[] bytes = this.classDefinitions.get(name);
            if(bytes == null) {
                throw new ClassNotFoundException("Local class definition not found for '" + name + "'");
            } else {
                return defineClass(null, bytes, 0, bytes.length);
            }
        }
    }

    void saveClass(DestinationClassObject compiled) {
        byte[] classBytes = compiled.getBytes();
        String binaryName = compiled.getBinaryName();
        saveClass(binaryName, classBytes);
    }

    void saveClass(String binaryName, byte[] classBytes) {
        String packageName = packageName(binaryName);
        this.classDefinitions.put(binaryName, classBytes);
        this.definedClasses.computeIfAbsent(packageName, k->new LinkedList<>()).add(binaryName);
    }


    private ClassPathJavaObject getLocallyDefined(String binaryName) {
        byte[] bytes = classDefinitions.get(binaryName);
        assert bytes != null;
        try {
            Class<?> cl = Class.forName(binaryName, false, this);
            return new ClassPathJavaObject(cl, bytes);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    Collection<JavaFileObject> getDefinedClasses(String packageName) {
        Collection<String> classNames = this.definedClasses.get(packageName);
        if(classNames == null) {
            return EMPTY;
        } else {
            return classNames.stream()
                    .map(this::getLocallyDefined)
                    .collect(Collectors.toList());
        }
    }
}
