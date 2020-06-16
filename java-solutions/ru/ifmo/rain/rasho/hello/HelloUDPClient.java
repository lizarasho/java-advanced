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
    private static final boolean LOGGING_ENABLED = true;
    private static final int REQUEST_TIMEOUT = 5;
    private static final int SO_TIMEOUT = 300;

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new RuntimeException("IP address of a host could not be determined: " + e.getMessage(), e);
        }

        SocketAddress socketAddress = new InetSocketAddress(inetAddress, port);

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
                final DatagramPacket request = HelloUDPUtils.newEmptyPacket(bufferSize, address);

                for (int requestIndex = 0; requestIndex < requests; requestIndex++) {
                    String requestBody = HelloUDPUtils.getRequestBody(threadIndex, requestIndex, prefix);
                    log("Request: " + requestBody);
                    HelloUDPUtils.setBody(request, requestBody);

                    boolean receivedValidResponse = false;
                    while (!receivedValidResponse && !Thread.interrupted() && !socket.isClosed()) {
                        try {
                            socket.send(request);
                            final DatagramPacket response = HelloUDPUtils.newEmptyPacket(bufferSize, address);
                            socket.receive(response);
                            String responseBody = HelloUDPUtils.getPacketBody(response);
                            receivedValidResponse = HelloUDPUtils.isValid(requestBody, responseBody);
                            log("Response: " + responseBody);
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

    private static void log(String message) {
        if (LOGGING_ENABLED) {
            System.out.println(message);
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Invalid input format: all arguments must be non-null. " + MAIN_USAGE);
        } else {
            int port = HelloUDPUtils.getIntegerArgumentSafely(args, 1, "port", MAIN_USAGE);
            int threadCount = HelloUDPUtils.getIntegerArgumentSafely(args, 3, "threads", MAIN_USAGE);
            int requestCount = HelloUDPUtils.getIntegerArgumentSafely(args, 4, "requests", MAIN_USAGE);
            new HelloUDPClient().run(args[0], port, args[2], threadCount, requestCount);
        }
    }
}