package ru.ifmo.rain.rasho.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that generates implementation of classes or interfaces and is capable of creating {@code .java} file.
 * Implementation class for {@link Impler} interface.
 *
 * @author Rasho Elizaveta
 */
public class Implementor implements Impler {
    /**
     * File extension of generated java files.
     */
    protected static final String JAVA = ".java";

    /**
     * Suffix in the name of the resulting file.
     */
    protected static final String CLASS_SUFFIX = "Impl";

    /**
     * Semicolon symbol for generated files.
     */
    private static final String SEMICOLON = ";";

    /**
     * Tab symbol for generated files.
     */
    private static final String TAB = "\t";

    /**
     * One space symbol for generated files.
     */
    private static final String SPACE = " ";

    /**
     * Comma symbol for generated files.
     */
    private static final String COMMA = ",";

    /**
     * Line separator depending on System for generated files.
     */
    private static final String LINE_SEP = System.lineSeparator();

    /**
     * Main method that is used to pass parameters of implementation.
     * <ul>
     * <li> Parameters format: {@code <classname> <root of output file>} </li>
     * </ul>
     * In case of incorrect arguments the error explanation is displayed.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        Objects.requireNonNull(args);

        if (args.length != 2) {
            System.err.println("Incorrect arguments: expected <classname> <root of output file>");
        } else {
            for (String arg : args) {
                Objects.requireNonNull(arg);
            }
            Impler implementor = new Implementor();
            try {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            } catch (ClassNotFoundException e) {
                System.err.println("Invalid class: " + e.getMessage());
            } catch (InvalidPathException e) {
                System.err.println("Invalid path: " + e.getMessage());
            } catch (ImplerException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    /**
     * Produces {@code .java} file implementing class or interface specified by provided {@code token}.
     * The location of resulting file is specified by path {@code root}.
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        Objects.requireNonNull(token);
        Objects.requireNonNull(root);

        if (canNotImplement(token)) {
            throw new ImplerException("Token cannot be implemented");
        }

        Path outputPath = getFilePath(token, root, JAVA);
        ImplementorUtils.createDirectories(outputPath);

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write(escape(getSource(token)));
        } catch (IOException e) {
            throw new ImplerException("Can't write into output file", e);
        }
    }

    /**
     * Returns if the implementation of {@code token} is possible.
     * Cases when implementation is impossible:
     * <ul>
     * <li> a primitive type </li>
     * <li> an array type </li>
     * <li> {@code Enum} </li>
     * <li> {@code private} modifier </li>
     * <li> {@code final} modifier </li>
     * </ul>
     *
     * @param token type token
     * @return {@code true} if {@code token} cannot be implemented
     */
    private static boolean canNotImplement(Class<?> token) {
        int modifiers = token.getModifiers();
        return token.isPrimitive() || token.isArray() || token == Enum.class ||
                Modifier.isPrivate(modifiers) || Modifier.isFinal(modifiers);
    }

    /**
     * Joins {@code declaration} and {@code body} parts.
     * The line indents are specified by {@code tabsNumber}.
     * <p>
     * The resulting view:
     * {@code declaration} {
     * {@code body}
     * }
     * </p>
     *
     * @param declaration the declaration of block
     * @param body        the body of block
     * @param tabsNumber  number of tabs in line indents
     * @return {@link String} representation of joined {@code declaration} and {@code body} implementation.
     */
    private static String createBlock(String declaration, String body, int tabsNumber) {
        return getTabs(tabsNumber) + declaration + SPACE + '{' + LINE_SEP +
                getTabs(tabsNumber + 1) + body + LINE_SEP +
                getTabs(tabsNumber) + '}' + LINE_SEP;
    }

    /**
     * Returns the {@code token} full implementation containing package declaration, class declaration and body.
     *
     * @param token type token
     * @return {@link String} representation of {@code token} implementation
     * @throws ImplerException in case of error during implementation
     */
    private static String getSource(Class<?> token) throws ImplerException {
        return getPackage(token) + getClassImpl(token);
    }

    /**
     * Returns the package declaration for {@code token}.
     *
     * @param token type token
     * @return {@link String} representation of {@code token} package declaration.
     */
    private static String getPackage(Class<?> token) {
        String tokenPackage = token.getPackageName();
        return tokenPackage.isEmpty() ? "" : ("package " + tokenPackage + SEMICOLON + LINE_SEP + LINE_SEP);
    }

    /**
     * Returns the {@code token} implementation containing class declaration and body.
     *
     * @param token type token
     * @return {@link String} representation of {@code token} implementation
     * @throws ImplerException if error occurs during implementation
     */
    private static String getClassImpl(Class<?> token) throws ImplerException {
        return createBlock(getClassDeclaration(token), getClassBody(token), 0);
    }


    /**
     * Returns the {@code token} implementation containing the body of class.
     *
     * @param token type token
     * @return {@link String} representation of {@code token} implementation
     * @throws ImplerException in case of error during implementation
     */
    private static String getClassBody(Class<?> token) throws ImplerException {
        StringBuilder result = new StringBuilder();
        if (!token.isInterface()) {
            result.append(getConstructorsImpl(token));
        }
        result.append(getAbstractMethodsImpl(token));
        return result.toString();
    }

    /**
     * Returns tab repeated {@code tabsNumber} times.
     *
     * @param tabsNumber number of tabs
     * @return {@link String} containing {@code tabsNumber} repeated tabs
     */
    private static String getTabs(final int tabsNumber) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < tabsNumber; i++) {
            result.append(TAB);
        }
        return result.toString();
    }

    /**
     * Returns annotation {@link Override} representation if the {@code executable} is a {@link Method}.
     *
     * @param executable provided {@link Method} or {@link Constructor}
     * @return {@link String} representation of annotation {@link Override} or empty {@link String}
     */
    private static String getOverride(Executable executable) {
        if (executable instanceof Constructor) {
            return "";
        }
        return "@Override" + LINE_SEP;
    }

    /**
     * Returns {@code value} and {@code name} of the {@code executable}.
     *
     * <ul>
     * <li> {@code return value} and {@code name} if the {@code executable} is an instance of {@link Method} </li>
     * <li> {@code name} if the {@code executable} is an instance of {@link Constructor} </li>
     * </ul>
     *
     * @param executable provided {@link Method} or {@link Constructor}
     * @return {@link String} implementation of {@code value} and {@code name}
     */
    private static String getReturnTypeAndName(Executable executable) {
        StringBuilder result = new StringBuilder();

        TypeVariable<?>[] parametersTypes = executable.getTypeParameters();
        if (parametersTypes.length > 0) {
            StringJoiner typesString = new StringJoiner(",", "<", "> ");
            for (TypeVariable<?> parameterType : parametersTypes) {
                typesString.add(parameterType.getTypeName());
            }
            result.append(typesString.toString());
        }

        if (executable instanceof Constructor) {
            Constructor<?> constructor = (Constructor<?>) executable;
            result.append(getClassName((constructor).getDeclaringClass()));
        } else {
            Method method = (Method) executable;
            result.append(method.getGenericReturnType().getTypeName().replaceAll(("\\$"), "."))
                    .append(SPACE)
                    .append(executable.getName());
        }

        return result.toString();
    }

    /**
     * Returns a {@link String} representation of parameter.
     *
     * @param type      generic parameter type
     * @param parameter specified parameter
     * @param typed     if the parameter need to be typed
     * @return {@link String} representation of parameter
     */
    private static String getParameter(Type type, Parameter parameter, boolean typed) {
        return (typed ? type.getTypeName().replace('$', '.') + SPACE : "")
                + parameter.getName();
    }

    /**
     * Returns a comma separated {@code parameters} in round parenthesises.
     *
     * @param executable executable which parameters are returned
     * @param typed      if the parameters need to be typed
     * @return {@link String} representation of the list of parameters
     */
    private static String getParameters(Executable executable, boolean typed) {
        StringJoiner result = new StringJoiner(COMMA + SPACE, "(", ")");

        Type[] types = executable.getGenericParameterTypes();
        Parameter[] parameters = executable.getParameters();

        for (int i = 0; i < parameters.length; i++) {
            result.add(getParameter(types[i], parameters[i], typed));
        }

        return result.toString();
    }


    /**
     * Returns the list of exceptions that can be thrown by specified {@code executable}.
     *
     * @param executable executable which exceptions are returned
     * @return {@link String} representation of the list of exceptions
     */
    private static String getExceptions(Executable executable) {
        Class<?>[] exceptions = executable.getExceptionTypes();
        if (exceptions.length == 0) {
            return "";
        }
        return "throws " + Arrays.stream(exceptions).map(Class::getCanonicalName).collect(Collectors.joining(", "));
    }

    /**
     * Returns default value of the object of class {@code token}.
     *
     * @param token the type token
     * @return default value of {@code token} class
     */
    private static String getDefaultValue(Class<?> token) {
        if (token.equals(boolean.class)) {
            return "false";
        }
        if (token.equals(void.class)) {
            return "";
        }
        if (token.isPrimitive()) {
            return "0";
        }
        return "null";
    }

    /**
     * Returns the {@code token} implementation containing the body of {@link Method} or {@link Constructor} executable.
     * In case of
     * <ul>
     * <li> {@link Method} returns default value of the {@code executable} return type </li>
     * <li> {@link Constructor} supers the parameters of {@code executable} </li>
     * </ul>
     *
     * @param executable instance of {@link Method} or {@link Constructor}
     * @return {@link String} implementation of the body
     */
    private static String getBodyImpl(Executable executable) {
        if (executable instanceof Constructor) {
            return "super" + getParameters(executable, false) + SEMICOLON;
        }
        String defaultValue = getDefaultValue(((Method) executable).getReturnType());
        return "return" + (defaultValue.isEmpty() ? "" : SPACE) + defaultValue + SEMICOLON;
    }

    /**
     * Returns the {@code executable} implementation containing {@link Method} or {@link Constructor} declaration and body.
     *
     * @param executable instance of {@link Method} or {@link Constructor}
     * @return {@link String} representation of {@code executable} implementation
     */
    private static String getExecutableImpl(Executable executable) {
        String modifiers = Modifier.toString(
                executable.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.NATIVE & ~Modifier.TRANSIENT
        );

        return getOverride(executable)
                + createBlock(
                modifiers + (modifiers.isEmpty() ? "" : SPACE) +
                        getReturnTypeAndName(executable) +
                        getParameters(executable, true) +
                        (getExceptions(executable).isEmpty() ? "" : SPACE) +
                        getExceptions(executable),
                getBodyImpl(executable),
                1
        );
    }


    /**
     * Returns the {@code token} implementation of non-private constructors.
     *
     * @param token type token
     * @return {@link String} representation of {@code token} non-private constructors implementation
     * @throws ImplerException if there are no non-private constructors
     */
    private static String getConstructorsImpl(Class<?> token) throws ImplerException {
        Constructor<?>[] constructors = Arrays.stream(token.getDeclaredConstructors())
                .filter(c -> !Modifier.isPrivate(c.getModifiers()))
                .toArray(Constructor<?>[]::new);

        if (constructors.length == 0) {
            throw new ImplerException("Class must have non-private constructor");
        }

        StringBuilder result = new StringBuilder();
        for (Constructor<?> constructor : constructors) {
            result.append(getExecutableImpl(constructor));
        }

        return result.toString();
    }

    /**
     * Returns name of the resulting file with {@code token} implementation.
     *
     * @param token type token
     * @return {@link String} representation of the resulting filename
     */
    protected static String getClassName(Class<?> token) {
        return token.getSimpleName() + CLASS_SUFFIX;
    }

    /**
     * Returns class declaration containing the resulting classname and base classname.
     *
     * @param token type token of base class
     * @return {@link String} class declaration representation of the resulting file
     */
    private static String getClassDeclaration(Class<?> token) {
        return "public class " + getClassName(token) +
                (token.isInterface() ? " implements " : " extends ") + token.getCanonicalName();
    }


    /**
     * Filters {@code methods} array leaving only abstract and final methods and stores wrapped methods in {@code abstractMethodsStorage} and {@code finalMethodsStorage}
     *
     * @param methods                array of {@link Method}
     * @param abstractMethodsStorage storage of filtered abstract methods
     * @param finalMethodsStorage    storage of filtered final methods
     */
    private static void getMethods(Method[] methods,
                                   Set<ImplementorUtils.MethodWrapper> abstractMethodsStorage,
                                   Set<ImplementorUtils.MethodWrapper> finalMethodsStorage) {
        Arrays.stream(methods)
                .filter(m -> Modifier.isFinal(m.getModifiers()))
                .map(ImplementorUtils.MethodWrapper::new)
                .collect(Collectors.toCollection(() -> finalMethodsStorage));

        Arrays.stream(methods)
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
                .map(ImplementorUtils.MethodWrapper::new)
                .collect(Collectors.toCollection(() -> abstractMethodsStorage));
    }

    /**
     * Returns the {@code token} abstract methods implementation in the resulting class.
     *
     * @param token type token
     * @return {@link String} representation of {@code token} methods implementation
     */
    private static String getAbstractMethodsImpl(Class<?> token) {
        Set<ImplementorUtils.MethodWrapper> abstractMethods = new HashSet<>();
        Set<ImplementorUtils.MethodWrapper> finalMethods = new HashSet<>();

        getMethods(token.getMethods(), abstractMethods, finalMethods);
        while (token != null) {
            getMethods(token.getDeclaredMethods(), abstractMethods, finalMethods);
            token = token.getSuperclass();
        }
        abstractMethods.removeAll(finalMethods);

        StringBuilder result = new StringBuilder();
        for (ImplementorUtils.MethodWrapper wrapper : abstractMethods) {
            result.append(getExecutableImpl(wrapper.getMethod()));
        }
        return result.toString();
    }

    /**
     * Returns path to the resulting file with {@code token} implementation.
     *
     * @param token type token
     * @param root  location of the class directory
     * @return {@link Path} representation of the path to file
     */
    protected static Path getFilePath(Class<?> token, Path root, String extension) {
        return root.resolve(
                Path.of(
                        token.getPackageName().replace('.', File.separatorChar),
                        getClassName(token) + extension
                )
        );
    }

    /**
     * Encodes the providing {@link String} source and escapes all unicode symbols.
     *
     * @param source {@link String} to encode
     * @return encoded {@link String}
     */
    private static String escape(String source) {
        StringBuilder result = new StringBuilder();
        for (char c : source.toCharArray()) {
            if (c >= 128) {
                result.append(String.format("\\u%04X", (int) c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}