package ru.ifmo.rain.rasho.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RecursiveWalk {

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Invalid input format: two non-null arguments are expected");
        } else {
            Path inputFilePath;
            Path outputFilePath;
            try {
                inputFilePath = Paths.get(args[0]);
                outputFilePath = Paths.get(args[1]);
                try {
                    if (outputFilePath.getParent() != null) {
                        Files.createDirectories(outputFilePath.getParent());
                    }
                    try (BufferedReader reader = Files.newBufferedReader(inputFilePath, StandardCharsets.UTF_8)) {
                        try (BufferedWriter writer = Files.newBufferedWriter(outputFilePath, StandardCharsets.UTF_8)) {
                            walkFile(reader, writer);
                        } catch (IOException e) {
                            System.err.println("Errors occur during opening the output file: " + e.getMessage());
                        }
                    } catch (FileNotFoundException e) {
                        System.err.println("Couldn't find the input file: " + e.getMessage());
                    } catch (IOException e) {
                        System.err.println("Errors occur during opening the input file: " + e.getMessage());
                    }
                } catch (IOException e) {
                    System.err.println("Couldn't access the output file: " + e.getMessage());
                }

            } catch (InvalidPathException e) {
                System.err.println("The path " + e.getInput() + " is invalid");
            }
        }
    }

    private static void walkFile(BufferedReader reader, BufferedWriter writer) throws IOException {
        RecursiveFileVisitor fileVisitor = new RecursiveFileVisitor(writer);
        String currentLine;
        while ((currentLine = reader.readLine()) != null) {
            try {
                Files.walkFileTree(Paths.get(currentLine), fileVisitor);
            } catch (InvalidPathException | IOException e) {
                writer.write("00000000 " + currentLine);
                writer.newLine();
            }
        }
    }
}