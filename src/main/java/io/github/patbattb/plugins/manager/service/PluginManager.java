package io.github.patbattb.plugins.manager.service;

import io.github.patbattb.plugins.core.Plugin;
import io.github.patbattb.plugins.core.expection.PluginCriticalException;
import io.github.patbattb.plugins.core.expection.PluginInterruptedException;
import io.github.patbattb.plugins.manager.exception.PluginNotFoundException;
import io.github.patbattb.plugins.manager.exception.PluginNotLoadedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PluginManager {

    private ConcurrentMap<String, Plugin> plugins;
    private final Logger log = LoggerFactory.getLogger(PluginManager.class);

    public PluginManager(PluginLoader loader) throws PluginNotLoadedException {
        loadPlugins(loader);
    }

    public ConcurrentMap<String, Plugin> getPlugins() {
        return plugins;
    }

    public Runnable getPluginRunnable(String pluginName) {
        return () -> {
            try {
                executePlugin(pluginName);
            } catch (PluginCriticalException e) {
                removePlugin(pluginName);
                log.error("Plugin {} has removed due to a critical error", pluginName);
            } catch (PluginInterruptedException e) {
                log.error("Plugin {} has interrupted.", pluginName);
            } catch (PluginNotFoundException e) {
                log.error("Plugin {} not found.", pluginName);
            }
        };
    }

    public void removePlugin(String name) {
        plugins.remove(name);
    }

    private void loadPlugins(PluginLoader loader) throws PluginNotLoadedException {
        Map<String, Plugin> map = loader.load();
        if (map == null || map.isEmpty()) {
            throw new PluginNotLoadedException("No one plugin has loaded.");
        }
        plugins = new ConcurrentHashMap<>(map);
    }

    private void executePlugin(String name) throws PluginCriticalException, PluginInterruptedException, PluginNotFoundException {
        Plugin plugin = plugins.get(name);
        if (plugin == null) {
            log.warn("Plugin {} not found in loading plugins.", name);
            throw new PluginNotFoundException("Plugin " + name + " not found in loading plugins.");
        }
        plugin.run();
    }
}
