package io.github.patbattb.plugins.manager.service;

import io.github.patbattb.plugins.core.Plugin;
import io.github.patbattb.plugins.core.expection.PluginCriticalException;
import io.github.patbattb.plugins.core.expection.PluginInterruptedException;
import io.github.patbattb.plugins.manager.exception.PluginNotLoadedException;
import io.github.patbattb.plugins.manager.smtp.MailClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

public class PluginManager {

    private CopyOnWriteArraySet<Plugin> plugins;
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

    public Set<Plugin> getPlugins() {
        return Set.copyOf(plugins);
    }

    public Runnable getPluginRunnable(@NotNull Plugin plugin) {
        return () -> {
            try {
                plugin.run();
            } catch (PluginCriticalException e) {
                removePlugin(plugin.getFullName());
                if (mailCriticalErrors) {
                    String subject = "Critical error in plugin: " + plugin.getTitle();
                    sendReport(subject, e);
                }
                log.error("Plugin {} has removed due to a critical error", plugin.getFullName(), e);
            } catch (PluginInterruptedException e) {
                if (mailInterrupterErrors) {
                    String subject = "Interrupt error in plugin: " + plugin.getTitle();
                    sendReport(subject, e);
                }
                log.error("Plugin {} has interrupted.", plugin.getFullName(), e);
            }
        };
    }

    public void removePlugin(String name) {
        plugins.removeIf(plugin -> plugin.getFullName().equals(name));
    }

    private void loadPlugins(PluginLoader loader) throws PluginNotLoadedException {
        Set<Plugin> map = loader.load();
        if (map == null) {
            throw new PluginNotLoadedException();
        }
        plugins = new CopyOnWriteArraySet<>(map);
    }

    private void sendReport(String subject, Exception exception) {
        String stackTrace = Arrays.stream(exception.getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.joining(System.lineSeparator()));
        String mailBody = exception.getMessage() + System.lineSeparator() + stackTrace;
        mailClient.sendEmail(subject, mailBody);
    }
}
