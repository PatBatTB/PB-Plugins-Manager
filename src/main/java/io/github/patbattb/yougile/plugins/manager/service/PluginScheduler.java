package io.github.patbattb.yougile.plugins.manager.service;

import io.github.patbattb.yougile.plugins.manager.exception.PluginAlreadyRunException;
import sun.misc.Signal;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PluginScheduler implements AutoCloseable {

    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final AtomicInteger exitCode = new AtomicInteger(0);

    private final PluginExecutor executor;
    private final PluginManager manager;
    private final int cycleTimeout;
    private final Map<String, Instant> nextRunTime;

    public PluginScheduler(PluginManager manager, PluginExecutor executor, int cycleTimeout) {
        this.manager = manager;
        this.executor = executor;
        this.cycleTimeout = cycleTimeout;
        this.nextRunTime = new HashMap<>();
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
            while (isRunning.get() && !nextRunTime.isEmpty()) {
                nextRunTime.entrySet().stream()
                        .filter(entry -> Instant.now().isAfter(entry.getValue()))
                        .forEach(entry -> {
                            String pluginName = entry.getKey();
                            Runnable runnable = manager.getPluginRunnable(pluginName);
                            try {
                                executor.invoke(pluginName, runnable);
                            } catch (PluginAlreadyRunException e) {
                                System.out.println(e.getMessage());
                            }
                            if (isRepeatable(pluginName)) {
                                updateNextRunTime(pluginName);
                            } else {
                                delete(pluginName);
                            }
                        });
                try {
                    TimeUnit.SECONDS.sleep(cycleTimeout);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void initializeSchedule() {
        if (manager != null && manager.getPlugins() != null && !manager.getPlugins().isEmpty()) {
            manager.getPlugins().forEach((k, v) -> nextRunTime.put(k, Instant.now()));
        }
    }

    private boolean isRepeatable(String pluginName) {
        return manager.getPlugins().containsKey(pluginName) && manager.getPlugins().get(pluginName).isRepeatable();
    }

    private void updateNextRunTime(String pluginName) {
        int timeout = manager.getPlugins().get(pluginName).timeout();
        Instant nextRun = Instant.now().plusSeconds(timeout);
        nextRunTime.put(pluginName, nextRun);
    }

    private void delete(String pluginName) {
        nextRunTime.remove(pluginName);
        manager.getPlugins().remove(pluginName);
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
        System.out.println("\nWaiting for the plug-ins to finish working");
        this.exitCode.set(exitCode);
        isRunning.set(false);
        executor.shutdown();
    }

    @Override
    public void close() {
        executor.close();
    }
}
