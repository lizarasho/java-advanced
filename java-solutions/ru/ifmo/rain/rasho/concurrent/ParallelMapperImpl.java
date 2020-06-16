package ru.ifmo.rain.rasho.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;


/**
 * The implementation of the {@link ParallelMapper} interface.
 *
 * @author Rasho Elizaveta
 */
@SuppressWarnings("unused")
public class ParallelMapperImpl implements ParallelMapper {

    private final Queue<Runnable> tasksToRun;
    private final List<Thread> threads;

    /**
     * Creates a {@code ParallelMapperImpl} instance operating with of {@code threadCount}
     * threads of type {@link Thread}.
     *
     * @param threadCount count of operable threads
     */
    public ParallelMapperImpl(int threadCount) {
        if (threadCount < 1) {
            throw new IllegalArgumentException("Incorrect number of threads: must be positive");
        }
        tasksToRun = new LinkedList<>();
        threads = new ArrayList<>();

        Thread thread = new Thread(
                () -> {
                    try {
                        while (!Thread.interrupted()) {
                            runTask();
                        }
                    } catch (InterruptedException ignored) {
                    }
                }
        );
        for (int i = 0; i < threadCount; i++) {
            threads.add(new Thread(thread));
        }
        threads.forEach(Thread::start);
    }

    private void runTask() throws InterruptedException {
        Runnable task;
        synchronized (tasksToRun) {
            while (tasksToRun.isEmpty()) {
                tasksToRun.wait();
            }
            task = tasksToRun.poll();
        }
        task.run();
    }

    private class MapResult<R> {
        private int nonNullTaskCount;
        private List<R> results;

        MapResult(int resultSize) {
            nonNullTaskCount = 0;
            results = new ArrayList<>(Collections.nCopies(resultSize, null));
        }

        synchronized void setByIndex(final int index, R element) {
            results.set(index, element);
            nonNullTaskCount++;
            if (nonNullTaskCount == results.size()) {
                notify();
            }
        }

        synchronized List<R> getResults() throws InterruptedException {
            while (nonNullTaskCount < results.size()) {
                wait();
            }
            return results;
        }

    }

    private void addTask(final Runnable task) {
        synchronized (tasksToRun) {
            tasksToRun.add(task);
            tasksToRun.notify();
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        MapResult<R> mapResult = new MapResult<>(args.size());
        List<RuntimeException> runtimeExceptions = new ArrayList<>();
        RuntimeException runtimeException = null;

        for (int i = 0; i < args.size(); i++) {
            final int index = i;
            addTask(
                    () -> {
                        R result = null;
                        try {
                            result = f.apply(args.get(index));
                        } catch (RuntimeException e) {
                            synchronized (runtimeExceptions) {
                                runtimeExceptions.add(e);
                            }
                        }
                        mapResult.setByIndex(index, result);
                    }
            );
        }

        if (runtimeExceptions.isEmpty()) {
            return mapResult.getResults();
        }

        RuntimeException totalException = new RuntimeException("Errors occurred during elements mapping");
        runtimeExceptions.forEach(totalException::addSuppressed);
        throw totalException;
    }

    @Override
    public void close() {
        threads.forEach(Thread::interrupt);
        threads.forEach(
                thread -> {
                    try {
                        thread.join();
                    } catch (InterruptedException ignored) {
                    }
                }
        );
    }
}
