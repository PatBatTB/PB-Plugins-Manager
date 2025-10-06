package io.github.patbattb.plugins.manager.service;

import io.github.patbattb.plugins.core.Plugin;
import io.github.patbattb.plugins.manager.exception.PluginNotLoadedException;

import java.util.Set;

@FunctionalInterface
public interface PluginLoader {
    Set<Plugin> load() throws PluginNotLoadedException;
}
