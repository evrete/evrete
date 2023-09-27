package org.evrete.runtime.compiler;

import javax.tools.JavaFileObject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RuntimeClassloader extends ClassLoader {

    private static final Collection<JavaFileObject> EMPTY = Collections.emptyList();
    private final Map<String, Collection<String>> definedClasses = new ConcurrentHashMap<>();

    private final Map<String, byte[]> classDefinitions = new ConcurrentHashMap<>();

    public RuntimeClassloader(ClassLoader parent) {
        super(parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        synchronized (this) {
            byte[] bytes = this.classDefinitions.get(name);
            if(bytes == null) {
                throw new ClassNotFoundException("Local class definition not found for '" + name + "'");
            } else {
                return defineClass(name, bytes, 0, bytes.length);
            }
        }
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        int pos = name.lastIndexOf(".class");
        if(pos < 0) {
            return super.getResourceAsStream(name);
        } else  {
            String noExtension = name.substring(0, pos);
            String binaryName = noExtension.replaceAll("/", ".");
            byte[] bytes = this.classDefinitions.get(binaryName);
            if(bytes == null) {
                return super.getResourceAsStream(name);
            } else {
                return new ByteArrayInputStream(bytes);
            }
        }
    }

    void saveClass(DestinationClassObject compiled) {
        byte[] classBytes = compiled.getBytes();
        String binaryName = compiled.getBinaryName();
        saveClass(binaryName, classBytes);
    }

    void saveClass(String binaryName, byte[] classBytes) {
        ClassMeta meta = new ClassMeta(binaryName);
        this.classDefinitions.put(binaryName, classBytes);
        this.definedClasses.computeIfAbsent(meta.getPackageName(), k->new LinkedList<>()).add(binaryName);
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
