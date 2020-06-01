package ru.ifmo.rain.rasho.hello;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

class HelloUDPUtils {

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
