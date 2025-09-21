package io.github.patbattb.yougile.plugins.manager.service;

import io.github.patbattb.yougile.plugins.manager.exception.PluginAlreadyRunException;
import sun.misc.Signal;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PluginScheduler {

    private AtomicBoolean isRunning = new AtomicBoolean(true);
    private AtomicInteger exitCode = new AtomicInteger(0);

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

    public void run() {
        try (PluginExecutor executor = this.executor) {
            while (isRunning.get()) {
                System.out.println("while starts");
                nextRunTime.entrySet().stream()
                        .filter(entry -> Instant.now().isAfter(entry.getValue()))
                        .forEach(entry -> {
                            String pluginName = entry.getKey();
                            try {
                                executor.invoke(pluginName);
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
                    throw new RuntimeException(e);
                }
                if (nextRunTime.isEmpty()) {
                    isRunning.set(false);
                }
            }
        }
    }

    private void initializeSchedule() {
        manager.getPlugins().forEach((k, v) -> nextRunTime.put(k, Instant.now()));
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
        exitCode.set(143);
        isRunning.set(false);
        executor.shutdown(exitCode.get());
    }
    private void shutdownInt(Signal signal) {
        exitCode.set(130);
        isRunning.set(false);
        executor.shutdown(exitCode.get());
    }

}
