package io.github.patbattb.yougile.plugins.manager.config;

import java.nio.file.Path;

public class Parameters {
    private final Path pluginsFolder = Path.of("plugins");

    public Path getPluginsFolder() {
        return pluginsFolder;
    }
}
