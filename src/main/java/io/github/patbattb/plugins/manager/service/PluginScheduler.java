package io.github.patbattb.plugins.manager.service;

import io.github.patbattb.plugins.core.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PluginScheduler implements AutoCloseable {

    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final AtomicInteger exitCode = new AtomicInteger(0);

    private final PluginExecutor executor;
    private final PluginManager manager;
    private final int cycleTimeout;
    private final ConcurrentMap<Plugin, Instant> schedule;

    private final Logger log = LoggerFactory.getLogger(PluginScheduler.class);

    public PluginScheduler(PluginManager manager, PluginExecutor executor, int cycleTimeout) {
        this.manager = manager;
        this.executor = executor;
        this.cycleTimeout = cycleTimeout;
        this.schedule = new ConcurrentHashMap<>();
        initializeSchedule();
        initShutdownSignals(executor);
    }

    public PluginScheduler(PluginManager manager, PluginExecutor executor) {
        this(manager, executor, 60);
    }

    public int getExitCode() {
        return exitCode.get();
    }

    public void run() {
        try (PluginExecutor executor = this.executor) {
            while (isRunning.get() && !schedule.isEmpty()) {
                Set<Plugin> plugins = manager.getPlugins();
                if (plugins.isEmpty()) {
                    log.warn("The manager has no more plugins to run. Scheduler is turning off.");
                    shutdown(0);
                    break;
                }
                log.debug("The scheduler is starting plugins.");
                for (Plugin plugin: plugins) {
                    if (!schedule.containsKey(plugin)) {
                        schedule.put(plugin, Instant.now());
                    }
                    Instant startTime = schedule.get(plugin);
                    if (startTime.isBefore(Instant.now()) || startTime.equals(Instant.now())) {
                        String pluginName = plugin.getFullName();
                        Runnable runnable = manager.getPluginRunnable(plugin);
                        log.debug("The plugin {} is starting.", pluginName);
                        executor.invoke(pluginName, runnable);
                        if (plugin.isRepeatable()) {
                            updateNextRunTime(plugin, plugin.timeout());
                        } else {
                            delete(plugin);
                        }
                    }
                }

                if (schedule.isEmpty()) {
                    log.warn("The scheduler has no more plugins to run. Scheduler is turning off.");
                    shutdown(0);
                    break;
                }

                try {
                    TimeUnit.SECONDS.sleep(cycleTimeout);
                } catch (InterruptedException e) {
                    log.warn("The running of the scheduler run-cycle has been interrupted.");
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void initializeSchedule() {
        if (manager != null && manager.getPlugins() != null && !manager.getPlugins().isEmpty()) {
            manager.getPlugins().forEach(plugin -> schedule.put(plugin, Instant.now()));
        }
    }

    private void updateNextRunTime(Plugin plugin, int timeout) {
        Instant nextRun = Instant.now().plusSeconds(timeout);
        schedule.put(plugin, nextRun);
    }

    private void delete(Plugin plugin) {
        schedule.remove(plugin);
        manager.removePlugin(plugin.getFullName());
    }

    private void initShutdownSignals(PluginExecutor executor) {
        Signal.handle(new Signal("TERM"), this::shutdownTerm);
        Signal.handle(new Signal("INT"), this::shutdownInt);
    }

    private void shutdownTerm(Signal signal) {
        shutdown(143);
    }
    private void shutdownInt(Signal signal) {
        shutdown(130);
    }

    private void shutdown(int exitCode) {
        log.debug("The signal to shutting down has been received. Waiting for the end of the running plugins.");
        this.exitCode.set(exitCode);
        isRunning.set(false);
        executor.shutdown();
    }

    @Override
    public void close() {
        executor.close();
    }
}
