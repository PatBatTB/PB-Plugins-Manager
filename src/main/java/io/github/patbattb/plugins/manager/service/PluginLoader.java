package io.github.patbattb.plugins.manager.service;

import io.github.patbattb.plugins.core.Plugin;
import io.github.patbattb.plugins.manager.exception.PluginNotLoadedException;

import java.util.Map;

@FunctionalInterface
public interface PluginLoader {
    Map<String, Plugin> load() throws PluginNotLoadedException;
}
