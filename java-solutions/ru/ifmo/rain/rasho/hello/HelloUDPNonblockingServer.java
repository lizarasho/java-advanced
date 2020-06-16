package ru.ifmo.rain.rasho.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;

public class HelloUDPNonblockingServer implements HelloServer {

    private static final String MAIN_USAGE = "Usage: HelloUDPNonblockingServer <port> <threads>";
    private static final int BUFFER_SIZE = 1024;
    private static final int TERMINATION_AWAIT_TIMEOUT = 1;
    private static final int REQUESTS_LIMIT = 1000;

    private Selector selector;
    private DatagramChannel datagramChannel;

    private ExecutorService mainExecutor;
    private ExecutorService executors;

    private Queue<ByteBuffer> emptyBuffers;
    private Queue<ByteBuffer> responseBuffers;
    private Queue<SocketAddress> responseAddresses;

    @Override
    public void start(int port, int threads) {
        emptyBuffers = new LinkedList<>();
        responseBuffers = new LinkedList<>();
        responseAddresses = new LinkedList<>();
        for (int i = 0; i < threads; i++) {
            emptyBuffers.add(ByteBuffer.allocate(BUFFER_SIZE));
        }
        executors = new ThreadPoolExecutor(
                threads,
                threads,
                0,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(REQUESTS_LIMIT),
                new ThreadPoolExecutor.DiscardPolicy()
        );
        mainExecutor = Executors.newSingleThreadExecutor();
        selector = Utils.getSelector();
        try {
            datagramChannel = Utils.newDatagramChannel();
            datagramChannel.bind(new InetSocketAddress(port));
            datagramChannel.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't initialize datagram channel: " + e.getMessage(), e);
        }
        mainExecutor.submit(this::process);
    }

    private void process() {
        while (!Thread.interrupted() && !datagramChannel.socket().isClosed()) {
            try {
                selector.select();
            } catch (IOException e) {
                throw new RuntimeException("IO errors occurred during keys selection: " + e.getMessage(), e);
            }
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            if (!selectedKeys.isEmpty()) {
                SelectionKey key = selectedKeys.iterator().next();
                selectedKeys.remove(key);
                if (key.isReadable()) {
                    receiveRequest();
                    key.interestOpsOr(SelectionKey.OP_WRITE);
                } else if (key.isWritable()) {
                    sendResponse();
                    key.interestOpsOr(SelectionKey.OP_READ);
                }
                if (emptyBuffers.isEmpty()) {
                    key.interestOpsAnd(~SelectionKey.OP_READ);
                } else if (responseBuffers.isEmpty()) {
                    key.interestOpsAnd(~SelectionKey.OP_WRITE);
                }
            }
        }
    }

    private void receiveRequest() {
        ByteBuffer buffer = emptyBuffers.remove();
        buffer.clear();
        SocketAddress address;
        try {
            address = datagramChannel.receive(buffer);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't receive datagram via channel: " + e.getMessage(), e);
        }
        buffer.flip();
        String requestBody = Utils.getStringFromByteBuffer(buffer);
        Future<String> futureResponseBody = executors.submit(() -> Utils.getResponseBody(requestBody));
        String responseBody;
        try {
            responseBody = futureResponseBody.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Errors occurred during parallel evaluation of '" + requestBody + "' response", e);
        }
        responseBuffers.add(Utils.newByteBuffer(responseBody));
        responseAddresses.add(address);
    }

    private void sendResponse() {
        ByteBuffer responseBuffer = responseBuffers.remove();
        try {
            datagramChannel.send(responseBuffer, responseAddresses.remove());
        } catch (IOException e) {
            throw new RuntimeException("Couldn't send response " + Utils.getStringFromByteBuffer(responseBuffer), e);
        }
        emptyBuffers.add(responseBuffer);
    }

    @Override
    public void close() {
        try {
            selector.close();
            datagramChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mainExecutor.shutdown();
        executors.shutdown();
        try {
            mainExecutor.awaitTermination(TERMINATION_AWAIT_TIMEOUT, TimeUnit.SECONDS);
            executors.awaitTermination(TERMINATION_AWAIT_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
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
        } else {
            int port = Utils.getIntegerArgumentSafely(args, 0, "port", MAIN_USAGE);
            int threadCount = Utils.getIntegerArgumentSafely(args, 1, "threads", MAIN_USAGE);
            new HelloUDPNonblockingServer().start(port, threadCount);
        }
    }
}