package ru.ifmo.rain.rasho.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Class that generates implementation of classes or interfaces and is capable of creating {@code .jar} file with solution.
 *
 * @author Rasho Elizaveta
 */
public class JarImplementor extends Implementor implements JarImpler {

    /**
     * File extension of compiled class files.
     */
    private static final String CLASS = ".class";

    /**
     * Main method that is used to pass parameters of implementation.
     * <ul>
     * <li> Parameters format: {@code -jar <classname> <.jar output file>}</li>
     * </ul>
     * In case of incorrect arguments the error explanation is displayed.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("-jar <classname> <.jar output file> is -jar mode");
        } else {
            for (String arg : args) {
                Objects.requireNonNull(arg);
            }
            JarImpler jarImplementor = new JarImplementor();
            try {
                jarImplementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            } catch (ImplerException e) {
            System.err.println(e.getMessage());
            } catch (ClassNotFoundException e) {
                System.err.println("Invalid class: " + e.getMessage());
            } catch (InvalidPathException e) {
                System.err.println("Invalid path: " + e.getMessage());
            }
        }
    }

    /**
     * Produces {@code .jar} file implementing class or interface specified by provided {@code token}.
     * The location of resulting file is specified by {@code jarFile}.
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        if (token == null) {
            throw new ImplerException("Token must be non-null");
        }
        if (jarFile == null) {
            throw new ImplerException("Jar-file path must be non-null");
        }
        ImplementorUtils.createDirectories(jarFile);

        Path tempDirectory;
        try {
            tempDirectory = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "jar-implementor");
        } catch (IOException e) {
            throw new ImplerException("Temporary directory cannot be created", e);
        }

        try {
            implement(token, tempDirectory);
            compile(token, tempDirectory);
            buildJarFile(token, jarFile, tempDirectory);
        } finally {
            ImplementorUtils.deleteDirectory(tempDirectory);
        }
    }

    /**
     * Compiles {@code .java} file specified by provided {@code token}.
     *
     * @param token     type token which implementation need to be compiled
     * @param directory location of resulting {@code .class} file
     * @throws ImplerException in case of compilation error.
     */
    private static void compile(Class<?> token, Path directory) throws ImplerException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Compilation error: compiler cannot be created");
        }

        String classPath;
        try {
            CodeSource codeSource = token.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                throw new ImplerException("Invalid CodeSource of token domain");
            }

            URL url = codeSource.getLocation();
            if (url == null) {
                throw new ImplerException("No URL was supplied during construction");
            }

            String urlPath = Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
            if (!urlPath.isEmpty()) {
                classPath = Path.of(urlPath).toString();
            } else {
                throw new ImplerException("Path part of URL doesn't exist");
            }
        } catch (URISyntaxException e) {
            throw new ImplerException("Failed to convert URL to URI", e);
        } catch (InvalidPathException | ImplerException e) {
            throw new ImplerException("Compilation error", e);
        }
        int exitCode = compiler.run(null, null, null,
                getFilePath(token, directory, JAVA).toString(),
                "-cp",
                classPath);
        if (exitCode != 0) {
            throw new ImplerException("Compilation failed with exit code " + exitCode);
        }
    }

    /**
     * Creates {@code .jar} file that contains compiled implementation of {@code token}.
     *
     * @param token         type token which implementation need to be archived in {@code .jar}
     * @param jarFile       location of resulting {@code .jar} file
     * @param tempDirectory directory containing compiled {@code .class} files
     * @throws ImplerException if error occurs during {@code .jar} creation
     */
    private static void buildJarFile(Class<?> token, Path jarFile, Path tempDirectory) throws ImplerException {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (JarOutputStream writer = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            String name = token.getPackageName().replace(".", "/") + "/" + getClassName(token) + CLASS;
            writer.putNextEntry(new ZipEntry(name));
            Files.copy(getFilePath(token, tempDirectory, CLASS), writer);

        } catch (IOException e) {
            throw new ImplerException("Error occurred during writing to JAR file", e);
        }
    }
}
