package com.maxdemarzi;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Magic {
    protected Magic() {}

    /**
     * Creates an instance of the given <var>type</var>, by calling the single-string-parameter constructor, or, if
     * the <var>value</var> equals "", the zero-parameter constructor.
     */
    public static Object
    createObject(Class<?> type, String value)
            throws NoSuchMethodException, InstantiationException, InvocationTargetException, IllegalAccessException {

        // Wrap primitive parameters.
        if (type.isPrimitive()) {
            type = (
                    type == boolean.class  ? Boolean.class
                            : type == char.class   ? Character.class
                              : type == byte.class   ? Byte.class
                                : type == short.class  ? Short.class
                                  : type == int.class    ? Integer.class
                                    : type == long.class   ? Long.class
                                      : type == float.class  ? Float.class
                                        : type == double.class ? Double.class
                                          : void.class
            );
        }

        // Construct object, assuming it has a default constructor or a
        // constructor with one single "String" argument.
        if ("".equals(value)) {
            return type.getConstructor(new Class[0]).newInstance(new Object[0]);
        } else {
            return type.getConstructor(String.class).newInstance(value);
        }
    }

    /**
     * @return <var>s</var>, split at the commas
     */
    public static String[]
    explode(String s) {
        StringTokenizer st = new StringTokenizer(s, ",");
        List<String>    l  = new ArrayList<String>();
        while (st.hasMoreTokens()) l.add(st.nextToken());
        return (String[]) l.toArray(new String[l.size()]);
    }

    /**
     * @return <var>s</var>, converted to a Java type
     */
    public static Class<?>
    stringToType(String s) {

        int brackets = 0;
        while (s.endsWith("[]")) {
            ++brackets;
            s = s.substring(0, s.length() - 2);
        }

        if (brackets == 0) {
            switch (s) {
                case "void" -> {
                    return void.class;
                }
                case "boolean" -> {
                    return boolean.class;
                }
                case "char" -> {
                    return char.class;
                }
                case "byte" -> {
                    return byte.class;
                }
                case "short" -> {
                    return short.class;
                }
                case "int" -> {
                    return int.class;
                }
                case "long" -> {
                    return long.class;
                }
                case "float" -> {
                    return float.class;
                }
                case "double" -> {
                    return double.class;
                }
                case "String" -> {
                    return String.class;
                }
            }
        }

        // Automagically convert primitive type names.
        switch (s) {
            case "void" -> s = "V";
            case "boolean" -> s = "Z";
            case "char" -> s = "C";
            case "byte" -> s = "B";
            case "short" -> s = "S";
            case "int" -> s = "I";
            case "long" -> s = "J";
            case "float" -> s = "F";
            case "double" -> s = "D";
            case "String" -> s = "Ljava.lang.String";
        }

        while (--brackets >= 0) s = '[' + s;
        try {
            return Class.forName(s);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            System.exit(1);
            throw new RuntimeException(); // Never reached. // SUPPRESS CHECKSTYLE AvoidHidingCause
        }
    }

    /**
     * Converts the given comma-separated list of class names to an array of {@link Class}es.
     */
    public static Class<?>[]
    stringToTypes(String s) {

        StringTokenizer st = new StringTokenizer(s, ",");
        List<Class<?>> l  = new ArrayList<Class<?>>();
        while (st.hasMoreTokens()) l.add(Magic.stringToType(st.nextToken()));
        Class<?>[] res = new Class[l.size()];
        l.toArray(res);
        return res;
    }
}
