package io.github.patbattb.yougile.plugins.manager.service;

import io.github.patbattb.yougile.plugins.core.YouGilePlugin;
import io.github.patbattb.yougile.plugins.core.expection.PluginCriticalException;
import io.github.patbattb.yougile.plugins.core.expection.PluginInterruptedException;
import io.github.patbattb.yougile.plugins.manager.exception.PluginNotFoundException;
import io.github.patbattb.yougile.plugins.manager.exception.PluginNotLoadedException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PluginManager {

    private ConcurrentMap<String, YouGilePlugin> plugins;

    public PluginManager(PluginLoader loader) throws PluginNotLoadedException {
        loadPlugins(loader);
    }

    public ConcurrentMap<String, YouGilePlugin> getPlugins() {
        return plugins;
    }

    private void loadPlugins(PluginLoader loader) throws PluginNotLoadedException {
        Map<String, YouGilePlugin> map = loader.load();
        if (map == null) {
            throw new PluginNotLoadedException();
        }
        plugins = new ConcurrentHashMap<>(map);
    }

    public Runnable getPluginRunnable(String pluginName) {
        return () -> {
            try {
                executePlugin(pluginName);
            } catch (PluginCriticalException e) {
                removePlugin(pluginName);
                System.out.println("Plugin" + pluginName + " has removed due to a critical error");
                e.printStackTrace();
            } catch (PluginInterruptedException e) {
                System.out.println("Plugin " + pluginName + " has interrupted.");
                e.printStackTrace();
            } catch (PluginNotFoundException e) {
                System.out.println("Plugin " + pluginName + " not found.");
                e.printStackTrace();
            }
        };
    }

    private void executePlugin(String name) throws PluginCriticalException, PluginInterruptedException, PluginNotFoundException {
        YouGilePlugin plugin = plugins.get(name);
        if (plugin == null) {
            throw new PluginNotFoundException("Plugin " + name + " not found in loading plugins.");
        }
        plugin.run();
    }

    public void removePlugin(String name) {
        plugins.remove(name);
    }
}
