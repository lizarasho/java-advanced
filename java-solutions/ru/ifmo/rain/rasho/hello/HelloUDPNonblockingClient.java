package ru.ifmo.rain.rasho.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class HelloUDPNonblockingClient implements HelloClient {

    private static final String MAIN_USAGE = "Usage: HelloUDPNonblockingClient <host> <port> <prefix> <threads> <requests>";
    private static final long SELECT_TIMEOUT = 200;
    private static final int BUFFER_SIZE = 1024;

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        SocketAddress socketAddress = Utils.getSocketAddress(host, port);
        Selector selector = Utils.getSelector();

        for (int threadIndex = 0; threadIndex < threads; threadIndex++) {
            try {
                DatagramChannel datagramChannel = Utils.newDatagramChannel();
                datagramChannel.connect(socketAddress);
                datagramChannel.register(selector, SelectionKey.OP_WRITE, new Context(threadIndex, 0));
            } catch (IOException e) {
                throw new RuntimeException("Couldn't initialize datagram channel: " + e.getMessage(), e);
            }
        }

        processAll(prefix, requests, selector, socketAddress);
    }

    private void processAll(String prefix, int requests, Selector selector, SocketAddress socketAddress) {
        while (!Thread.interrupted() && !selector.keys().isEmpty()) {
            try {
                selector.select(SELECT_TIMEOUT);
            } catch (IOException e) {
                throw new RuntimeException("IO errors occurred during keys selection: " + e.getMessage(), e);
            }
            final Set<SelectionKey> selectedKeys = selector.selectedKeys();
            if (!selectedKeys.isEmpty()) {
                for (final Iterator<SelectionKey> iterator = selectedKeys.iterator(); iterator.hasNext(); ) {
                    final SelectionKey key = iterator.next();
                    if (key.isReadable()) {
                        processResponse(requests, prefix, key);
                    } else if (key.isWritable()) {
                        processRequest(prefix, key, socketAddress);
                    }
                    iterator.remove();
                }
            } else {
                for (final SelectionKey key : selector.keys()) {
                    if (key.isWritable()) {
                        processRequest(prefix, key, socketAddress);
                    }
                }
            }
        }
    }

    private void processResponse(int requests, final String prefix, final SelectionKey key) {
        DatagramChannel datagramChannel = (DatagramChannel) key.channel();
        Context context = (Context) key.attachment();
        ByteBuffer buffer = context.getBuffer();
        buffer.clear();
        try {
            datagramChannel.receive(buffer);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't receive datagram via channel: " + e.getMessage(), e);
        }
        buffer.flip();
        String responseBody = Utils.getStringFromByteBuffer(buffer);
        if (Utils.isValid(context.getThreadIndex(), context.getRequestIndex(), responseBody)) {
            String requestBody = Utils.getRequestBody(context.getThreadIndex(), context.getRequestIndex(), prefix);
            Utils.log("Request: " + requestBody);
            Utils.log("Response: " + responseBody);
            context.incRequestIndex();
        }
        if (context.getRequestIndex() != requests) {
            key.interestOps(SelectionKey.OP_WRITE);
        } else {
            try {
                key.channel().close();
            } catch (IOException e) {
                throw new RuntimeException("Couldn't close channel: " + e.getMessage(), e);
            }
        }
    }

    private void processRequest(final String prefix, SelectionKey key, final SocketAddress socketAddress) {
        DatagramChannel channel = (DatagramChannel) key.channel();
        Context context = (Context) key.attachment();
        ByteBuffer buffer = context.getBuffer();
        buffer.clear();
        String requestBody = Utils.getRequestBody(context.getThreadIndex(), context.getRequestIndex(), prefix);
        try {
            channel.send(Utils.newByteBuffer(requestBody), socketAddress);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't send request " + requestBody, e);
        }
        buffer.flip();
        key.interestOps(SelectionKey.OP_READ);
    }

    private static class Context {
        private final int threadIndex;
        private int requestIndex;
        private final ByteBuffer buffer;

        Context(int threadIndex, int requestIndex) {
            this.threadIndex = threadIndex;
            this.requestIndex = requestIndex;
            this.buffer = ByteBuffer.allocate(BUFFER_SIZE);
        }

        int getThreadIndex() {
            return threadIndex;
        }

        int getRequestIndex() {
            return requestIndex;
        }

        void incRequestIndex() {
            this.requestIndex++;
        }

        public ByteBuffer getBuffer() {
            return buffer;
        }
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
        } else {
            int port = Utils.getIntegerArgumentSafely(args, 1, "port", MAIN_USAGE);
            int threadCount = Utils.getIntegerArgumentSafely(args, 3, "threads", MAIN_USAGE);
            int requestCount = Utils.getIntegerArgumentSafely(args, 4, "requests", MAIN_USAGE);
            new HelloUDPNonblockingClient().run(args[0], port, args[2], threadCount, requestCount);
        }
    }
}