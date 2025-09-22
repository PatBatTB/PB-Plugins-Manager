package io.github.patbattb.plugins.manager.service;

import io.github.patbattb.plugins.core.Plugin;
import io.github.patbattb.plugins.manager.exception.PluginNotLoadedException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class JarPluginLoader implements PluginLoader {

    private final Path pluginsFolder;
    private final Map<String, Plugin> plugins = new HashMap<>();

    public JarPluginLoader(Path pluginsFolder) {
        this.pluginsFolder = pluginsFolder;
    }

    public Map<String, Plugin> load() throws PluginNotLoadedException {
        try (Stream<Path> jarPaths = Files.list(pluginsFolder)) {
            jarPaths.forEach(this::processJar);
        } catch (IOException e) {
            throw new PluginNotLoadedException("No one plugin loaded.");
        }
        if (plugins.isEmpty()) {
            throw new PluginNotLoadedException("No one plugin loaded.");
        }
        return plugins;
    }

    private void processJar(Path jarPath) {
        if (!jarPath.toString().endsWith(".jar")) {
            return;
        }
        try (URLClassLoader classLoader = getNewClassLoader(jarPath.toUri().toURL())){
            Plugin plugin = getPlugin(jarPath, classLoader);
            if (plugins.containsKey(plugin.getFullName())) {
                throw new PluginNotLoadedException("Plugins collision! This class "+plugin.getFullName()+" is already registered.");
            }
            plugins.put(plugin.getFullName(), plugin);
        } catch (PluginNotLoadedException | IOException e) {
            //TODO need logging
            System.out.println(e.getMessage());
        }
    }

    private URLClassLoader getNewClassLoader(URL jarUrl) {
        return new URLClassLoader(new URL[]{jarUrl}, getClass().getClassLoader());
    }

    private Plugin getPlugin(Path jarPath, URLClassLoader classLoader) throws PluginNotLoadedException {
        Optional<Plugin> resultOptional = Optional.empty();
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            int counter = 0;
            while(entries.hasMoreElements()) {
                if (counter > 1) {
                    throw new PluginNotLoadedException(jarPath.getFileName() +
                            " contains a few classes inherited from YouGilePlugin. The only one class is allowed"
                    );
                }
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }
                String className = getRealClassName(jarFile, entry);
                Optional<Plugin> pluginOptional = extractPlugin(classLoader, className);
                if (pluginOptional.isPresent()) {
                    resultOptional = pluginOptional;
                    counter++;
                }
            }
        } catch (IOException e) {
            throw new PluginNotLoadedException(e);
        }
        return resultOptional.orElseThrow(() ->
                new PluginNotLoadedException(
                        "The class inherited from YouGilePlugin in "+jarPath.getFileName()+" not found"
                )
        );
    }

    private Optional<Plugin> extractPlugin(URLClassLoader classLoader, String className) throws PluginNotLoadedException {
        try {
            Class<?> clazz = classLoader.loadClass(className);
            Class<?> superClazz = clazz.getSuperclass();
            if (Plugin.class.isAssignableFrom(superClazz)) {
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                return Optional.of((Plugin) constructor.newInstance());

            }
        } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException |
                 InstantiationException | IllegalAccessException e) {
            throw new PluginNotLoadedException(e);
        }
        return Optional.empty();
    }

    private String getRealClassName(JarFile jarFile, JarEntry jarEntry) throws PluginNotLoadedException {
        try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
            ClassReader reader = new ClassReader(inputStream);
            ClassNameVisitor visitor = new ClassNameVisitor();
            reader.accept(visitor, ClassReader.SKIP_CODE);
            return visitor.getClassName();
        } catch (IOException e) {
            throw new PluginNotLoadedException("Error of getting class name.", e);
        }
    }

    private static class ClassNameVisitor extends ClassVisitor {

        private String className;

        public ClassNameVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.className = name.replace("/", ".");
        }

        public String getClassName() {
            return className;
        }
    }
}
