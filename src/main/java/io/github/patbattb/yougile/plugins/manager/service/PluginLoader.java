package io.github.patbattb.yougile.plugins.manager.service;

import io.github.patbattb.yougile.plugins.core.YouGilePlugin;

import java.util.concurrent.ConcurrentMap;

@FunctionalInterface
public interface PluginLoader {
    ConcurrentMap<String, YouGilePlugin> load();
}
