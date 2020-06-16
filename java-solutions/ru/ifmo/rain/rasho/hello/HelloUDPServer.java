package ru.ifmo.rain.rasho.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.*;

public class HelloUDPServer implements HelloServer {

    private static final String MAIN_USAGE = "Usage: HelloUDPServer <port> <threads>";
    private static final boolean LOGGING_ENABLED = false;
    private static final int TERMINATION_AWAIT_TIMEOUT = 1;
    private static final int REQUESTS_LIMIT = 1000;

    private DatagramSocket socket;
    private ExecutorService listener;
    private ExecutorService executors;

    @Override
    public void start(int port, int threads) {
        try {
            socket = new DatagramSocket(port);
            executors = new ThreadPoolExecutor(
                    threads,
                    threads,
                    0,
                    TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(REQUESTS_LIMIT),
                    new ThreadPoolExecutor.DiscardPolicy()
            );
            listener = Executors.newSingleThreadExecutor();
            int receiveBufferSize = socket.getReceiveBufferSize();
            listener.submit(() -> {
                try {
                    while (!Thread.interrupted() && !socket.isClosed()) {
                        final DatagramPacket request = Utils.newEmptyPacket(receiveBufferSize);
                        socket.receive(request);
                        executors.submit(sendResponse(request));
                    }
                } catch (IOException e) {
                    if (!socket.isClosed()) {
                        throw new RuntimeException("Error occurred during request receiving: " + e.getMessage(), e);
                    }
                }
            });
        } catch (SocketException e) {
            throw new RuntimeException("There is an error in the underlying protocol: " + e.getMessage(), e);
        }
    }

    private Runnable sendResponse(DatagramPacket request) {
        return () -> {
            final String requestBody = Utils.getPacketBody(request);
            log("Request: " + requestBody);
            String responseBody = Utils.getResponseBody(requestBody);
            DatagramPacket response = Utils.newEmptyPacket(request.getSocketAddress());
            Utils.setBody(response, Utils.getResponseBody(requestBody));
            log("Response: " + responseBody);
            try {
                socket.send(response);
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    throw new RuntimeException("Error occurred during response sending: " + e.getMessage(), e);
                }
            }
        };
    }

    @Override
    public void close() {
        listener.shutdown();
        executors.shutdown();
        socket.close();
        try {
            listener.awaitTermination(TERMINATION_AWAIT_TIMEOUT, TimeUnit.SECONDS);
            executors.awaitTermination(TERMINATION_AWAIT_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    private static void log(String message) {
        if (LOGGING_ENABLED) {
            System.out.println(message);
        }
    }

    public static void main(String[] args) {
        if (args == null) {
            throw new IllegalArgumentException("Expected non-null arguments. " + MAIN_USAGE);
        }
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected 2 non-null arguments, found " + args.length + ". " + MAIN_USAGE);
        }
        if (args[0] == null || args[1] == null) {
            throw new IllegalArgumentException("Invalid input format: all arguments must be non-null. " + MAIN_USAGE);
        }
        int port = Utils.getIntegerArgumentSafely(args, 0, "port", MAIN_USAGE);
        int threadCount = Utils.getIntegerArgumentSafely(args, 1, "threads", MAIN_USAGE);
        new HelloUDPNonblockingServer().start(port, threadCount);
    }
}