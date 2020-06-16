package ru.ifmo.rain.rasho.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPClient implements HelloClient {

    private static final String MAIN_USAGE = "Usage: HelloUDPClient <host> <port> <prefix> <threads> <requests>";
    private static final int REQUEST_TIMEOUT = 5;
    private static final int SO_TIMEOUT = 300;

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        SocketAddress socketAddress = Utils.getSocketAddress(host, port);

        ExecutorService executors = Executors.newFixedThreadPool(threads);
        for (int threadIndex = 0; threadIndex < threads; threadIndex++) {
            executors.submit(processRequestAndResponse(socketAddress, threadIndex, prefix, requests));
        }
        executors.shutdown();
        try {
            executors.awaitTermination(threads * requests * REQUEST_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    private Runnable processRequestAndResponse(final SocketAddress address, int threadIndex, String prefix, int requests) {
        return () -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(SO_TIMEOUT);

                int bufferSize = socket.getReceiveBufferSize();
                final DatagramPacket request = Utils.newEmptyPacket(bufferSize, address);

                for (int requestIndex = 0; requestIndex < requests; requestIndex++) {
                    String requestBody = Utils.getRequestBody(threadIndex, requestIndex, prefix);
                    Utils.log("Request: " + requestBody);
                    Utils.setBody(request, requestBody);

                    boolean receivedValidResponse = false;
                    while (!receivedValidResponse && !Thread.interrupted() && !socket.isClosed()) {
                        try {
                            socket.send(request);
                            final DatagramPacket response = Utils.newEmptyPacket(bufferSize, address);
                            socket.receive(response);
                            String responseBody = Utils.getPacketBody(response);
                            receivedValidResponse = Utils.isValid(requestBody, responseBody);
                            Utils.log("Response: " + responseBody);
                        } catch (IOException e) {
                            System.err.println("Error occurred during request sending or response receiving: " + e.getMessage());
                        }
                    }
                }
            } catch (SocketException e) {
                System.err.println("The socket could not be opened: " + e.getMessage());
            }
        };
    }

    public static void main(String[] args) {
        if (args == null) {
            throw new IllegalArgumentException("Expected non-null arguments. " + MAIN_USAGE);
        }
        if (args.length != 5) {
            throw new IllegalArgumentException("Expected 5 non-null arguments, found " + args.length + ". " + MAIN_USAGE);
        }
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Invalid input format: all arguments must be non-null. " + MAIN_USAGE);
        }
        int port = Utils.getIntegerArgumentSafely(args, 1, "port", MAIN_USAGE);
        int threadCount = Utils.getIntegerArgumentSafely(args, 3, "threads", MAIN_USAGE);
        int requestCount = Utils.getIntegerArgumentSafely(args, 4, "requests", MAIN_USAGE);
        new HelloUDPNonblockingClient().run(args[0], port, args[2], threadCount, requestCount);
    }
}