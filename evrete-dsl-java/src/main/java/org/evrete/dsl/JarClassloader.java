package org.evrete.dsl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

class JarClassloader extends URLClassLoader {
    private final Collection<URL> resources;

    JarClassloader(Collection<URL> resources, ClassLoader parent) {
        super(resources.toArray(new URL[0]), parent);
        this.resources = resources;
    }

    void scan(Consumer<Class<?>> consumer) throws IOException {
        for (URL url : resources) {
            scan(url, consumer);
        }
    }

    private void scan(URL url, Consumer<Class<?>> consumer) throws IOException {
        URLConnection connection = url.openConnection();

        try(InputStream is = connection.getInputStream()) {
            JarInputStream stream = new JarInputStream(is);
            JarEntry next = stream.getNextJarEntry();
            while (next != null) {
                String entryName = next.getName();
                if (entryName.endsWith(".class") && !entryName.equals("module-info.class")) {
                    String className = entryName.substring(0, entryName.length() - 6).replace('/', '.');
                    try {
                        Class<?> cl = loadClass(className);
                        consumer.accept(cl);
                    } catch (ClassNotFoundException e) {
                        throw new MalformedResourceException("Unable to load class " + className, e);
                    }
                }
                next = stream.getNextJarEntry();
            }
        }
    }

}
