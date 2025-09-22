package io.github.patbattb.yougile.plugins.manager.service;

import io.github.patbattb.yougile.plugins.core.YouGilePlugin;
import io.github.patbattb.yougile.plugins.manager.exception.PluginNotLoadedException;

import java.util.Map;

@FunctionalInterface
public interface PluginLoader {
    Map<String, YouGilePlugin> load() throws PluginNotLoadedException;
}
