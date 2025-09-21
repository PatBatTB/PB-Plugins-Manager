package io.github.patbattb.yougile.plugins.manager.service;

import io.github.patbattb.yougile.plugins.core.YouGilePlugin;

import java.util.Map;

@FunctionalInterface
public interface PluginLoader {
    Map<String, YouGilePlugin> load();
}
