package org.evrete.runtime.compiler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class RuntimeClassloader extends ClassLoader {

    //private final Map<String, Collection<String>> definedClasses = new HashMap<>();

    private final Map<String, byte[]> classDefinitions = new HashMap<>();

    public RuntimeClassloader(ClassLoader parent) {
        super(parent);
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

    public void saveClass(String binaryName, byte[] classBytes) {
        super.defineClass(binaryName, classBytes, 0, classBytes.length);
        this.classDefinitions.put(binaryName, classBytes);
    }
}
