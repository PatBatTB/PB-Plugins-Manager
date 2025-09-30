package io.github.patbattb.plugins.manager.service;

import io.github.patbattb.plugins.core.Plugin;
import io.github.patbattb.plugins.manager.exception.PluginNotLoadedException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
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
    private final Logger log = LoggerFactory.getLogger(JarPluginLoader.class);

    public JarPluginLoader(Path pluginsFolder) {
        this.pluginsFolder = pluginsFolder;
    }

    public Map<String, Plugin> load() throws PluginNotLoadedException {
        try (Stream<Path> jarPaths = Files.list(pluginsFolder)) {
            jarPaths.forEach(this::processJar);
        } catch (IOException e) {
            log.error("Couldn't read pluginsFolder: {}", pluginsFolder);
            throw new PluginNotLoadedException("No one plugin loaded.", e);
        }
        if (plugins.isEmpty()) {
            log.error("No one plugin loaded.");
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
            log.error(e.getMessage(), e);
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
                            " contains a few classes inherited from Plugin class. The only one class is allowed");
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
                        "The class inherited from Plugin class in "+jarPath.getFileName()+" not found."
                )
        );
    }

    private Optional<Plugin> extractPlugin(URLClassLoader classLoader, String className) {
        try {
            Class<?> clazz = classLoader.loadClass(className);
            Class<?> superClazz = clazz.getSuperclass();
            if (superClazz != null && Plugin.class.isAssignableFrom(superClazz)) {
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                return Optional.of((Plugin) constructor.newInstance());

            }
        } catch (Exception | LinkageError e) {
            log.debug("The {} class couldn't read", className);
        }
        return Optional.empty();
    }

    private String getRealClassName(JarFile jarFile, JarEntry jarEntry) throws IOException {
        ClassNameVisitor visitor = new ClassNameVisitor();
        try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
            ClassReader reader = new ClassReader(inputStream);
            reader.accept(visitor, ClassReader.SKIP_CODE);
        } catch (IOException e) {
            log.debug("Couldn't read class name.");
            throw new IOException(e);
        }
        return visitor.getClassName();
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
