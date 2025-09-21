package io.github.patbattb.yougile.plugins.manager.service;

import io.github.patbattb.yougile.plugins.core.expection.PluginCriticalException;
import io.github.patbattb.yougile.plugins.core.expection.PluginInterruptedException;
import io.github.patbattb.yougile.plugins.manager.exception.PluginAlreadyRunException;
import io.github.patbattb.yougile.plugins.manager.exception.PluginNotFoundException;

import java.util.concurrent.*;

public class PluginExecutor {

    private final PluginManager pluginManager;
    private final ExecutorService executorService;
    private final ConcurrentMap<String, Future<?>> tasks;
    private final Object lock;

    public PluginExecutor(PluginManager pluginManager, int threadPool) {
        this.pluginManager = pluginManager;
        this.executorService = Executors.newFixedThreadPool(threadPool);
        this.tasks = new ConcurrentHashMap<>();
        this.lock = new Object();
    }

    public void invoke(String pluginName) throws PluginAlreadyRunException {
        synchronized (lock) {
            Future<?> task;
            if (tasks.containsKey(pluginName)) {
                task = tasks.get(pluginName);
                if (task != null && !task.isDone()) {
                    throw new PluginAlreadyRunException("Task " + pluginName + " ignored - already running");
                }
            }
            task = executorService.submit(getTask(pluginName));
            tasks.put(pluginName, task);
        }
    }

    private Runnable getTask(String pluginName) {
        return () -> {
            try {
                pluginManager.executePlugin(pluginName);
            } catch (PluginCriticalException e) {
                pluginManager.removePlugin(pluginName);
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

    public void shutdown(int exitCode) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                System.out.println("ExecutorService is shutting down immediately.");
                executorService.shutdownNow();
            }
            System.exit(exitCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
