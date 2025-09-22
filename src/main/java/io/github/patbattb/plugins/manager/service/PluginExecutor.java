package io.github.patbattb.plugins.manager.service;

import io.github.patbattb.plugins.manager.exception.PluginAlreadyRunException;

import java.util.concurrent.*;

public class PluginExecutor implements AutoCloseable {

    private static final int DEFAULT_THREAD_POOL = 10;

    private final ExecutorService executorService;
    private final ConcurrentMap<String, Future<?>> tasks;
    private final Object lock;
    private int terminationTimeout = 30;

    public PluginExecutor(int threadPool) {
        this.executorService = Executors.newFixedThreadPool(threadPool);
        this.tasks = new ConcurrentHashMap<>();
        this.lock = new Object();
    }

    public PluginExecutor() {
        this(DEFAULT_THREAD_POOL);
    }

    public void setTerminationTimeout(int terminationTimeout) {
        this.terminationTimeout = terminationTimeout;
    }

    public void invoke(String pluginName, Runnable runnable) throws PluginAlreadyRunException {
        synchronized (lock) {
            Future<?> task;
            if (isTaskRunning(pluginName)) {
                throw new PluginAlreadyRunException("Task " + pluginName + " ignored - already running");
            }
            task = executorService.submit(runnable);
            tasks.put(pluginName, task);
        }
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(terminationTimeout, TimeUnit.SECONDS)) {
                System.out.println("ExecutorService is shutting down immediately.");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        executorService.close();
    }

    private boolean isTaskRunning(String pluginName) {
        if (tasks.containsKey(pluginName)) {
            Future<?> task = tasks.get(pluginName);
            return task != null && !task.isDone();
        }
        return false;
    }
}
