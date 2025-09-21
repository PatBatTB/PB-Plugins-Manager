package io.github.patbattb.yougile.plugins.manager.service;

import io.github.patbattb.yougile.plugins.core.YouGilePlugin;
import io.github.patbattb.yougile.plugins.core.expection.PluginCriticalException;
import io.github.patbattb.yougile.plugins.core.expection.PluginInterruptedException;
import io.github.patbattb.yougile.plugins.manager.exception.PluginNotFoundException;

import java.util.concurrent.ConcurrentMap;

public class PluginManager {

    private ConcurrentMap<String, YouGilePlugin> plugins;

    public ConcurrentMap<String, YouGilePlugin> getPlugins() {
        return plugins;
    }

    public void loadPlugins(PluginLoader loader) {
        plugins = loader.load();
    }

    public void executePlugin(String name) throws PluginCriticalException, PluginInterruptedException, PluginNotFoundException {
        YouGilePlugin plugin = plugins.get(name);
        if (plugin == null) {
            throw new PluginNotFoundException();
        }
        plugin.run();
    }

    public void removePlugin(String name) {
        plugins.remove(name);
    }
}
