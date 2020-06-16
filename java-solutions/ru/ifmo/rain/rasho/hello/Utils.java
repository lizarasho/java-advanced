package ru.ifmo.rain.rasho.hello;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;

class Utils {
    private static final boolean LOGGING_ENABLED = true;

    static void log(String message) {
        if (LOGGING_ENABLED) {
            System.out.println(message);
        }
    }

    static DatagramPacket newEmptyPacket(final int bufferSize) {
        return new DatagramPacket(new byte[bufferSize], bufferSize);
    }

    static DatagramPacket newEmptyPacket(final int bufferSize, final SocketAddress address) {
        return new DatagramPacket(new byte[bufferSize], bufferSize, address);
    }

    static DatagramPacket newEmptyPacket(final SocketAddress address) {
        return new DatagramPacket(new byte[0], 0, address);
    }

    static String getRequestBody(final int threadIndex, final int requestIndex, final String prefix) {
        return prefix + threadIndex + '_' + requestIndex;
    }

    static String getResponseBody(final String requestBody) {
        return "Hello, " + requestBody;
    }

    static void setBody(final DatagramPacket packet, String body) {
        packet.setData(body.getBytes(StandardCharsets.UTF_8));

    }

    static String getPacketBody(DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

    static boolean isValid(String requestBody, String responseBody) {
        return responseBody.contains(requestBody);
    }

    static SocketAddress getSocketAddress(String host, int port) {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new RuntimeException("IP address of a host could not be determined: " + e.getMessage(), e);
        }
        return new InetSocketAddress(inetAddress, port);
    }

    static Selector getSelector() {
        Selector selector;
        try {
            selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't open selector: " + e.getMessage(), e);
        }
        return selector;
    }

    static DatagramChannel newDatagramChannel() throws IOException {
        DatagramChannel datagramChannel = DatagramChannel.open();
        datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        datagramChannel.configureBlocking(false);
        return datagramChannel;
    }

    static String getStringFromByteBuffer(ByteBuffer byteBuffer) {
        return StandardCharsets.UTF_8.decode(byteBuffer).toString();
    }

    static ByteBuffer newByteBuffer(String source) {
        return ByteBuffer.wrap(source.getBytes(StandardCharsets.UTF_8));
    }

    static boolean isValid(int threadIndex, int requestIndex, final String response) {
        int threadIndexLeft = readNotDigits(0, response);
        int threadIndexRight = readDigits(threadIndexLeft, response);
        int requestIndexLeft = readNotDigits(threadIndexRight, response);
        int requestIndexRight = readDigits(requestIndexLeft, response);
        return checkEquality(threadIndex, response, threadIndexLeft, threadIndexRight)
                && checkEquality(requestIndex, response, requestIndexLeft, requestIndexRight);
    }

    private static boolean checkEquality(int n, String response, int leftIndex, int rightIndex) {
        return Integer.toString(n).equals(response.substring(leftIndex, rightIndex));
    }

    private static int readNotDigits(int beginIndex, String response) {
        return readSymbols(beginIndex, false, response);
    }

    private static int readDigits(int beginIndex, String response) {
        return readSymbols(beginIndex, true, response);
    }

    private static int readSymbols(int beginIndex, boolean readDigits, String response) {
        int index = beginIndex;
        while (index < response.length() && Character.isDigit(response.charAt(index)) == readDigits) {
            index++;
        }
        return index;
    }

    static int getIntegerArgumentSafely(String[] args, int index, String identifier, String usage) {
        int result;
        try {
            result = Integer.parseInt(args[index]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Expected integer number of " + identifier + ", found " + args[index] + ": " + e.getMessage() + '\n' + usage
            );
        }
        return result;
    }
}
