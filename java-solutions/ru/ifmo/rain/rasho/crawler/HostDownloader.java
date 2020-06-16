package ru.ifmo.rain.rasho.crawler;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

class HostDownloader {

    private final ExecutorService downloadersPool;
    private final Queue<Runnable> waitingTasks;
    private final int perHost;
    private int activeTasks;

    HostDownloader(final int perHost, final ExecutorService downloadersPool) {
        this.downloadersPool = downloadersPool;
        this.perHost = perHost;

        activeTasks = 0;
        waitingTasks = new LinkedList<>();
    }

    synchronized private void tryCallNext() {
        if (activeTasks < perHost) {
            Runnable task = waitingTasks.poll();
            if (task != null) {
                activeTasks++;
                downloadersPool.submit(() -> {
                    try {
                        task.run();
                    } finally {
                        activeTasks--;
                        tryCallNext();
                    }
                });
            }
        }
    }

    synchronized void add(final Runnable task) {
        waitingTasks.add(task);
        tryCallNext();
    }
}
