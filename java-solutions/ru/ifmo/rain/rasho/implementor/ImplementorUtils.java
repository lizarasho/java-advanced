package ru.ifmo.rain.rasho.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Objects;

/**
 * Class containing some utilities for {@link Implementor}.
 */
class ImplementorUtils {

    private ImplementorUtils() { }

    /**
     * Removes the specified {@code directory}.
     *
     * @param directory root of directory to be deleted
     * @throws ImplerException if the error during deleting of directory occurred
     */
    static void deleteDirectory(Path directory) throws ImplerException {
        if (Files.exists(directory)) {
            try {
                Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                throw new ImplerException("Temporary directory cannot be removed", e);
            }
        }
    }

    /**
     * Creates parent directory for {@code file}
     *
     * @param file file to create parent directory
     * @throws ImplerException if the directory for the {@code file} cannot be created
     */
    static void createDirectories(Path file) throws ImplerException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new ImplerException("Directory for file cannot be created", e);
            }
        }
    }


    /**
     * A wrapper class for {@link Method}.
     */
    static class MethodWrapper {

        /**
         * The wrapped {@link Method} object.
         */
        private final Method method;

        /**
         * Creates a new {@link MethodWrapper} instance with wrapped {@code method}.
         *
         * @param method the method to be wrapped
         */
        MethodWrapper(Method method) {
            this.method = method;
        }

        /**
         * Getter method for wrapped instance of {@link Method}.
         *
         * @return wrapped {@link #method}
         */
        Method getMethod() {
            return method;
        }

        /**
         * Compares the passed object with this wrapper for equality.
         * Returns true if both methods have the same names and parameters' types.
         *
         * @param obj the reference object with which to compare
         * @return {@code true} if specified obj is equal to this {@link MethodWrapper}; {@code false} otherwise
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            MethodWrapper that = (MethodWrapper) obj;
            return method.getName().equals(that.method.getName())
                    && Arrays.equals(method.getParameterTypes(), that.getMethod().getParameterTypes());
        }

        /**
         * Calculates hashcode for this {@link MethodWrapper} using custom hash based on {@link #method} name and parameters' types.
         *
         * @return calculated hashcode for this {@link MethodWrapper}
         */
        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(method.getParameterTypes()), method.getName());
        }
    }
}
