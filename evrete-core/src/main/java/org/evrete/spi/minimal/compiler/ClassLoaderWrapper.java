package org.evrete.spi.minimal.compiler;

import javax.tools.JavaFileObject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;

class ClassLoaderWrapper extends ClassLoader {

    private final Map<String, byte[]> classDefinitions = new HashMap<>();
    private final Map<String, Collection<JavaFileObject>> fileObjects = new HashMap<>();

    ClassLoaderWrapper(ClassLoader parent) {
        super(parent);
    }


    @Override
    public InputStream getResourceAsStream(String name) {
        int pos = name.lastIndexOf(".class");
        if (pos < 0) {
            return super.getResourceAsStream(name);
        } else {
            String noExtension = name.substring(0, pos);
            String binaryName = noExtension.replaceAll("/", ".");
            byte[] bytes = this.classDefinitions.get(binaryName);
            if (bytes == null) {
                return super.getResourceAsStream(name);
            } else {
                return new ByteArrayInputStream(bytes);
            }
        }
    }

    Collection<JavaFileObject> getDefinedClasses(String packageName) {
        Collection<JavaFileObject> result = new LinkedList<>();
        walk(new Consumer<ClassLoaderWrapper>() {
            @Override
            public void accept(ClassLoaderWrapper classLoaderWrapper) {
                result.addAll(classLoaderWrapper.getLocallyDefinedClasses(packageName));
            }
        });
        return result;
    }

    void walk(Consumer<ClassLoaderWrapper> consumer) {
        consumer.accept(this);

        ClassLoader current = getParent();
        while (current != null) {
            if(current instanceof ClassLoaderWrapper) {
                consumer.accept((ClassLoaderWrapper) current);
            }
            current = current.getParent();
        }
    }

    Collection<JavaFileObject> getLocallyDefinedClasses(String packageName) {
        return this.fileObjects.getOrDefault(packageName, Collections.emptyList());
    }

    void defineNewClass(String binaryName, byte[] classBytes) {
        String packageName = packageNameOf(binaryName);
        Class<?> defined = super.defineClass(binaryName, classBytes, 0, classBytes.length);
        this.classDefinitions.put(binaryName, classBytes);
        this.fileObjects.computeIfAbsent(packageName, k -> new ArrayList<>()).add(new ClassPathJavaObject(defined, classBytes));
    }

    private String packageNameOf(String className) {
        int lastDot = className.lastIndexOf('.');
        return (lastDot == -1) ? null : className.substring(0, lastDot);
    }
}
