package io.github.patbattb.plugins.manager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class PluginExecutor implements AutoCloseable {

    private static final int DEFAULT_THREAD_POOL = 10;

    private final ExecutorService executorService;
    private final ConcurrentMap<String, Future<?>> tasks;
    private final Object lock;
    private int terminationTimeout = 30;
    private final Logger log = LoggerFactory.getLogger(PluginExecutor.class);

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

    public void invoke(String pluginName, Runnable runnable) {
        synchronized (lock) {
            Future<?> task;
            if (isTaskRunning(pluginName)) {
                log.debug("The Running of {} has been ignored. The last executing of this plugin has not ended yet.",
                        pluginName
                );
            }
            task = executorService.submit(runnable);
            tasks.put(pluginName, task);
        }
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(terminationTimeout, TimeUnit.SECONDS)) {
                log.debug("Await termination timeout. The executor service stops immediately.");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.debug("The executor service's shutdown was interrupted.");
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
