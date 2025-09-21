package io.github.patbattb.yougile.plugins.manager;

import io.github.patbattb.yougile.plugins.manager.config.Parameters;
import io.github.patbattb.yougile.plugins.manager.service.JarPluginLoader;
import io.github.patbattb.yougile.plugins.manager.service.PluginExecutor;
import io.github.patbattb.yougile.plugins.manager.service.PluginManager;
import sun.misc.Signal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class App {
    static AtomicBoolean aBoolean = new AtomicBoolean(true);
    static AtomicInteger exitCode = new AtomicInteger(0);
    static PluginExecutor executor;

    private static final PluginManager MANAGER = new PluginManager();

    public static void main(String[] args) {
        Signal.handle(new Signal("TERM"), App::shutdownTerm);
        Signal.handle(new Signal("INT"), App::shutdownInt);
        executor = new PluginExecutor(MANAGER, 10);
        Parameters params = new Parameters();
        initialization(params);
        JarPluginLoader loader = new JarPluginLoader(params.getPluginsFolder());
        MANAGER.loadPlugins(loader);

    }

    private static void initialization(Parameters parameters) {
        try {
            createPluginsFolder(parameters.getPluginsFolder());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createPluginsFolder(Path pluginFolder) throws IOException {
        if (!Files.exists(pluginFolder)) {
            Files.createDirectory(pluginFolder);
        }
    }

    public static void shutdownTerm(Signal signal) {
        exitCode.set(143);
        aBoolean.set(false);
        executor.shutdown(exitCode.get());
    }
    public static void shutdownInt(Signal signal) {
        exitCode.set(130);
        aBoolean.set(false);
        executor.shutdown(exitCode.get());
    }

}