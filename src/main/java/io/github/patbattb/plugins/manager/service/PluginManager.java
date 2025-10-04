package io.github.patbattb.plugins.manager.service;

import io.github.patbattb.plugins.core.Plugin;
import io.github.patbattb.plugins.core.expection.PluginCriticalException;
import io.github.patbattb.plugins.core.expection.PluginInterruptedException;
import io.github.patbattb.plugins.manager.exception.PluginNotFoundException;
import io.github.patbattb.plugins.manager.exception.PluginNotLoadedException;
import io.github.patbattb.plugins.manager.smtp.MailClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PluginManager {

    private ConcurrentMap<String, Plugin> plugins;
    private final MailClient mailClient;
    private final boolean mailInterrupterErrors;
    private final boolean mailCriticalErrors;
    private final Logger log = LoggerFactory.getLogger(PluginManager.class);

    public PluginManager(PluginLoader loader) throws PluginNotLoadedException {
        this(loader, null, false, false);
    }

    public PluginManager(PluginLoader loader, MailClient mailClient,
                         boolean mailInterruptedErrors, boolean mailCriticalErrors) throws PluginNotLoadedException {
        loadPlugins(loader);
        this.mailClient = mailClient;
        this.mailInterrupterErrors = mailInterruptedErrors;
        this.mailCriticalErrors = mailCriticalErrors;
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
                if (mailCriticalErrors) {
                    String subject = "Critical error in plugin: " + pluginName;
                    sendReport(subject, e);
                }
                log.error("Plugin {} has removed due to a critical error", pluginName, e);
            } catch (PluginInterruptedException e) {
                if (mailInterrupterErrors) {
                    String subject = "Interrupt error in plugin: " + pluginName;
                    sendReport(subject, e);
                }
                log.error("Plugin {} has interrupted.", pluginName, e);
            } catch (PluginNotFoundException e) {
                log.error("Plugin {} not found.", pluginName, e);
            }
        };
    }

    public void removePlugin(String name) {
        plugins.remove(name);
    }

    private void loadPlugins(PluginLoader loader) throws PluginNotLoadedException {
        Map<String, Plugin> map = loader.load();
        if (map == null) {
            throw new PluginNotLoadedException();
        }
        plugins = new ConcurrentHashMap<>(map);
    }

    private void executePlugin(String name) throws PluginCriticalException, PluginInterruptedException, PluginNotFoundException {
        Plugin plugin = plugins.get(name);
        if (plugin == null) {
            log.warn("Plugin {} not found in loading plugins.", name);
            throw new PluginNotFoundException();
        }
        plugin.run();
    }

    private void sendReport(String subject, Exception exception) {
        String mailBodyBuilder = exception.getMessage() +
                System.lineSeparator() +
                Arrays.toString(exception.getStackTrace());
        mailClient.sendEmail(subject, mailBodyBuilder);
    }
}
