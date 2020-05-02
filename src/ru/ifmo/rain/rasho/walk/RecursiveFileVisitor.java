package ru.ifmo.rain.rasho.walk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveFileVisitor extends SimpleFileVisitor<Path> {

    private static final int ZERO_VALUE = 0x811c9dc5;
    private static final int FNV_MULTIPLIER = 0x01000193;
    private static final int BUFFER_SIZE = 1024;

    private final BufferedWriter writer;

    RecursiveFileVisitor(BufferedWriter writer) {
        this.writer = writer;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        try (InputStream reader = Files.newInputStream(file)) {
            writer.write(String.format("%08x %s", getHash(reader), file.toString()));
        } catch (InvalidPathException | IOException e) {
            writer.write("00000000 " + file.toString());
        }
        writer.newLine();
        return super.visitFile(file, attrs);
    }

    private int getHash(InputStream reader) throws IOException {
        byte[] fileBuffer = new byte[BUFFER_SIZE];
        int result;
        int hash = ZERO_VALUE;
        while ((result = reader.read(fileBuffer)) != -1) {
            for (int i = 0; i < result; i++) {
                hash = (hash * FNV_MULTIPLIER) ^ (fileBuffer[i] & 0xff);
            }
        }
        return hash;
    }
}
