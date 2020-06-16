package ru.ifmo.rain.rasho.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {

    private static final String MAIN_USAGE = "Usage: WebCrawler url [depth [downloaders [extractors [perHost]]]]";
    private static final int DEFAULT_VALUE = 1;

    private final Downloader downloader;
    private final int perHost;

    private final ExecutorService extractorsPool;
    private final ExecutorService downloadersPool;
    private final ConcurrentMap<String, HostDownloader> hostDownloaders;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;

        downloadersPool = Executors.newFixedThreadPool(downloaders);
        extractorsPool = Executors.newFixedThreadPool(extractors);
        hostDownloaders = new ConcurrentHashMap<>();
    }

    @Override
    public Result download(String url, int depth) {
        return new RecursiveDownloader().download(url, depth);
    }

    @Override
    public void close() {
        extractorsPool.shutdown();
        downloadersPool.shutdown();
    }

    private class RecursiveDownloader {

        private final CopyOnWriteArrayList<String> downloaded;
        private final ConcurrentMap<String, IOException> errors;

        private final Set<String> processedLinks;
        private final ConcurrentLinkedQueue<String> linksQueue;

        RecursiveDownloader() {
            downloaded = new CopyOnWriteArrayList<>();
            linksQueue = new ConcurrentLinkedQueue<>();
            errors = new ConcurrentHashMap<>();
            processedLinks = ConcurrentHashMap.newKeySet();
        }

        Result download(final String url, final int depth) {
            linksQueue.add(url);
            for (int i = 0; i < depth; i++) {
                List<String> currentDepthLinks = new ArrayList<>(linksQueue);
                linksQueue.clear();

                final Phaser phaser = new Phaser(1);
                try {
                    final int restDepth = depth - i;
                    currentDepthLinks.stream()
                            .parallel()
                            .filter(processedLinks::add)
                            .forEach(link -> processLink(link, restDepth, phaser));
                } finally {
                    phaser.arriveAndAwaitAdvance();
                }
            }
            return new Result(downloaded, errors);
        }

        private void updateQueue(final Document document, final Phaser phaser) {
            phaser.register();
            extractorsPool.submit(() -> {
                try {
                    linksQueue.addAll(document.extractLinks());
                } catch (IOException ignored) {
                } finally {
                    phaser.arrive();
                }
            });
        }

        void processLink(final String link, final int restDepth, final Phaser phaser) {
            String host;
            try {
                host = URLUtils.getHost(link);
                HostDownloader hostDownloader = hostDownloaders.computeIfAbsent(host, s -> new HostDownloader(perHost, downloadersPool));
                phaser.register();
                hostDownloader.add(() -> {
                    try {
                        Document document = downloader.download(link);
                        if (restDepth > 1) {
                            updateQueue(document, phaser);
                        }
                        downloaded.add(link);
                    } catch (IOException e) {
                        errors.put(link, e);
                    } finally {
                        phaser.arrive();
                    }
                });
            } catch (MalformedURLException e) {
                errors.put(link, e);
            }
        }
    }

    private static int getIntegerArgument(final String[] args, final int index) {
        if (index < args.length) {
            return Integer.parseInt(args[index]);
        }
        return DEFAULT_VALUE;
    }

    public static void main(String[] args) {
        if (args == null || args.length == 0 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Invalid input format: arguments must be non-null. " + MAIN_USAGE);
        } else {
            try {
                String url = args[0];
                int depth = getIntegerArgument(args, 1);
                int downloaders = getIntegerArgument(args, 2);
                int extractors = getIntegerArgument(args, 3);
                int perHost = getIntegerArgument(args, 4);

                try (Crawler crawler = new WebCrawler(new CachingDownloader(), downloaders, extractors, perHost)) {
                    crawler.download(url, depth);
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid input format: depth, downloaders, extractors and perHost must be non-null. \n" + MAIN_USAGE);
            } catch (IOException e) {
                System.err.println("Error in caching downloader initialization occurred: " + e.getMessage());
            }
        }
    }
}