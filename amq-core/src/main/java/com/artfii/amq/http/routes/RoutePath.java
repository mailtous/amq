package com.artfii.amq.http.routes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * RoutePath
 *
 * @author leeton
 * @since 1.0
 */
public class RoutePath {
    private RoutePath() {

    }

    public static int parameterCount(String path) {
        if (null == path ) throw new IllegalArgumentException();

        Matcher matcher = Route.PARAMETER_PATTERN.matcher(path);
        int count = 0;
        while (matcher.find())
            count++;
        return count;
    }

    public static List<String> parameters(String path) {
        if (null == path )throw new IllegalArgumentException();

        List<String> parameters = new ArrayList<>();
        Matcher matcher = Route.PARAMETER_PATTERN.matcher(path);
        int count = 0;
        while (matcher.find())
            parameters.add(matcher.group(1));
        return parameters;
    }

    public static List<String> addMethodParms(List<String> paramList, List<Parameter> methodParms) {
        methodParms.stream().filter(m -> null != m.getName()).forEach((m) -> paramList.add(m.getName()));
        return paramList;
    }


    public static Method of(String methodPath) throws ClassNotFoundException {
        return RoutePath.of(methodPath, 0);
    }

    public static Method of(String methodPath, int parameterCount) throws ClassNotFoundException {
        int index;
        if (null == methodPath || (index = methodPath.indexOf('@')) == -1 || index == methodPath.length() - 1)
            throw new IllegalArgumentException("Method path must be in the format ClassName@methodName");
        return RoutePath.of(methodPath.substring(0, index), methodPath.substring(index + 1), parameterCount);
    }

    public static Method of(String parent, String method) throws ClassNotFoundException {
        return RoutePath.of(ClassLoader.getSystemClassLoader().loadClass(parent), method);
    }

    public static Method of(String parent, String method, int parameterCount) throws ClassNotFoundException {
        return RoutePath.of(ClassLoader.getSystemClassLoader().loadClass(parent), method, parameterCount);
    }

    public static Method of(Class<?> parent, String method) {
        return RoutePath.of(parent, method, 0);
    }

    public static Method of(Class<?> parent, String method, int parameterCount) {
        if (null == parent || null == method || parameterCount < 0)
            throw new IllegalArgumentException();
        Class<?> c = parent;
        do {
            for (Method m : c.getMethods())
                if (method.equals(m.getName()) && m.getParameterCount() == parameterCount)
                    return m;
            for (Method m : c.getDeclaredMethods())
                if (method.equals(m.getName()) && m.getParameterCount() == parameterCount)
                    return m;
        } while ((c = c.getSuperclass()) != null);
        throw new RuntimeException(
                String.format("Unable to find method mapping to route[parent=%s, method=%s, parameterCount=%s].",
                        parent, method, parameterCount));
    }
}
