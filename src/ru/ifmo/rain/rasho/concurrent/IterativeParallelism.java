package ru.ifmo.rain.rasho.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The implementation of the {@link AdvancedIP} interface representing methods for parallel processing.
 *
 * @author Rasho Elizaveta
 */
@SuppressWarnings("unused")
public class IterativeParallelism implements AdvancedIP {

    private final ParallelMapper mapper;

    /**
     * Default constructor.
     * Creates an new {@code IterativeParallelism} instance operating without {@link ParallelMapper}.
     */
    public IterativeParallelism() {
        mapper = null;
    }

    /**
     * Mapper constructor.
     * Creates an new {@code IterativeParallelism} instance operating with {@link ParallelMapper} as a mapper.
     *
     * @param mapper {@link ParallelMapper} instance
     */
    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    private <T> List<Stream<T>> shareTasks(int threads, List<T> tasks) {
        List<Stream<T>> sharedTasks = new ArrayList<>();
        int threadTasks = tasks.size() / threads;
        int addTasks = tasks.size() - threadTasks * threads;

        for (int i = 0, fromIndex = 0; i < threads; i++) {
            int toIndex = fromIndex + threadTasks;
            if (addTasks > i) {
                toIndex++;
            }
            sharedTasks.add(tasks.subList(fromIndex, toIndex).stream());
            fromIndex = toIndex;
        }

        return sharedTasks;
    }

    private <T, U, R> R baseOperation(int threadCount, List<T> values,
                                      Function<Stream<T>, U> task,
                                      Function<Stream<U>, R> resReduce) throws InterruptedException {
        if (threadCount < 1) {
            throw new IllegalArgumentException("Incorrect number of threads: must be positive");
        }
        threadCount = Math.min(threadCount, values.size());

        List<Stream<T>> sharedTasks = shareTasks(threadCount, values);
        List<U> threadResult;

        if (mapper != null) {
            threadResult = mapper.map(task, sharedTasks);
        } else {
            List<Thread> threads = new ArrayList<>();
            threadResult = new ArrayList<>(Collections.nCopies(threadCount, null));
            for (int i = 0; i < threadCount; i++) {
                final int finalPos = i;
                Thread thread = new Thread(() -> threadResult.set(finalPos, task.apply(sharedTasks.get(finalPos))));
                threads.add(thread);
                thread.start();
            }
            joinThreads(threads);
        }

        return resReduce.apply(threadResult.stream());
    }

    private void joinThreads(List<Thread> threads) throws InterruptedException {
        InterruptedException exception = null;
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                if (exception == null) {
                    exception = e;
                }
                exception.addSuppressed(e);
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return baseOperation(threads, values,
                stream -> max(stream, comparator),
                stream -> max(stream, comparator));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private <T> T max(Stream<T> stream, Comparator<? super T> comparator) {
        return stream.max(comparator).get();
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, Collections.reverseOrder(comparator));
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return baseOperation(threads, values,
                stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(Boolean::booleanValue));
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return baseOperation(threads, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    private <T, R> List<R> createBijectionByFunction(Function<Stream<? extends T>, Stream<? extends R>> bijectionFunction,
                                                     int threads, List<? extends T> values) throws InterruptedException {
        return baseOperation(threads, values,
                stream -> bijectionFunction.apply(stream).collect(Collectors.toList()),
                stream -> stream.flatMap(List::stream).collect(Collectors.toList()));
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return createBijectionByFunction(stream -> stream.filter(predicate), threads, values);
    }

    @Override
    public <T, R> List<R> map(int threads, List<? extends T> values, Function<? super T, ? extends R> function) throws InterruptedException {
        return createBijectionByFunction(stream -> stream.map(function), threads, values);
    }

    private <T> T getMonoidReduce(Stream<T> stream, Monoid<T> monoid) {
        return stream.reduce(monoid.getIdentity(), monoid.getOperator());
    }

    @Override
    public <T> T reduce(int threads, List<T> values, Monoid<T> monoid) throws InterruptedException {
        return baseOperation(threads, values,
                stream -> getMonoidReduce(stream, monoid),
                stream -> getMonoidReduce(stream, monoid));
    }

    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, Monoid<R> monoid) throws InterruptedException {
        return baseOperation(threads, values,
                stream -> stream.map(lift).reduce(monoid.getIdentity(), monoid.getOperator()),
                stream -> getMonoidReduce(stream, monoid));
    }
}