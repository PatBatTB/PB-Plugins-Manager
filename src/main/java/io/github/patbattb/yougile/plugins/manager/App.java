package io.github.patbattb.yougile.plugins.manager;

import io.github.patbattb.yougile.plugins.manager.config.Parameters;
import io.github.patbattb.yougile.plugins.manager.service.PluginManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class App {
    private static final PluginManager MANGER = new PluginManager();

    public static void main(String[] args) {
        Parameters params = new Parameters();
        initialization(params);

    }

    private static void initialization(Parameters parameters) {
        try {
            createPluginsFolder(parameters.getPluginsFolder());
            MANGER.loadPlugins(parameters.getPluginsFolder());
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