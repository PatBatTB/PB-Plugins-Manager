package io.github.patbattb.yougile.plugins.manager;

import io.github.patbattb.yougile.plugins.manager.config.Parameters;
import io.github.patbattb.yougile.plugins.manager.exception.PluginNotLoadedException;
import io.github.patbattb.yougile.plugins.manager.service.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class App {

    public static void main(String[] args) {
        Parameters params = new Parameters();
        initialization(params);


        PluginLoader loader = new JarPluginLoader(params.getPluginsFolder());
        PluginManager manager;
        try {
            manager = new PluginManager(loader);
        } catch (PluginNotLoadedException e) {
            throw new RuntimeException(e);
        }
        try (PluginExecutor executor = new PluginExecutor(manager)) {
            executor.setTerminationTimeout(1);
            PluginScheduler scheduler = new PluginScheduler(manager, executor, 1);
            scheduler.run();
        }

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

}