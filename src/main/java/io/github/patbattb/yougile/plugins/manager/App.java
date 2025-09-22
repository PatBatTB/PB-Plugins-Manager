package io.github.patbattb.yougile.plugins.manager;

import io.github.patbattb.yougile.plugins.manager.exception.PluginNotLoadedException;
import io.github.patbattb.yougile.plugins.manager.service.*;

import java.nio.file.Path;

public class App {

    public static void main(String[] args) {

        PluginLoader loader = new JarPluginLoader(Path.of("plugins"));
        PluginManager manager;
        try {
            manager = new PluginManager(loader);
        } catch (PluginNotLoadedException e) {
            throw new RuntimeException(e);
        }
        try (PluginScheduler scheduler = new PluginScheduler(manager, new PluginExecutor(), 1)) {
            scheduler.run();
            System.exit(scheduler.getExitCode());
        }

    }
}