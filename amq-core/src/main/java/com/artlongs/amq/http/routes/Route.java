package com.artlongs.amq.http.routes;

import com.artlongs.amq.http.HttpHandler;
import com.artlongs.amq.http.HttpRequest;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Route
 *
 * @author leeton
 * @since 1.0
 */
public class Route {
    public static final Logger logger = LoggerFactory.getLogger(Route.class);
    public static final String DEFAULT_PARAMETER_PATTERN = ".+";
    public static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{([A-z]+)}");
    private final String path;
    private final List<String> pathParams = new ArrayList<>();
    private final Map<Integer, String> reversePathParamsOrder = new HashMap<>();
    private final Map<String, Integer> pathParamsOrder = new HashMap<>();
    private final Map<String, String> parameterPatterns = new HashMap<>();
    private final String requestType;
    private HttpHandler direct;
    private Method method;
    private Pattern evaluatedPattern;
    private Object parent;

    public Route(String requestType, String path) {
        if (null == requestType || null == path)
            throw new IllegalArgumentException();
        this.requestType = requestType;
        this.path = path;
        Matcher matcher = PARAMETER_PATTERN.matcher(path);
        int i = 0;
        while (matcher.find()) {
            String parameter = matcher.group(1);
            pathParams.add(parameter);
            pathParamsOrder.put(parameter, i);
            reversePathParamsOrder.put(i, parameter);
            parameterPatterns.put(parameter, DEFAULT_PARAMETER_PATTERN);
            i++;
        }
    }

    public static Route get(String path) {
        return new Route(HttpRequest.METHOD_GET, path);
    }

    public static Route post(String path) {
        return new Route(HttpRequest.METHOD_POST, path);
    }

    public static Route delete(String path) {
        return new Route(HttpRequest.METHOD_DELETE, path);
    }

    public String requestType() {
        return requestType;
    }

    public String path() {
        return path;
    }

    public Route where(String parameter, String pattern) {
        if (parameter == null || pattern == null)
            throw new IllegalArgumentException();
        if (!parameterPatterns.containsKey(parameter))
            throw new IllegalArgumentException("Unknown parameter: " + parameter);
        parameterPatterns.put(parameter, pattern);
        evaluatedPattern = null;
        return this;
    }

    public Route where(String parameter, int index) {
        if (!pathParamsOrder.containsKey(parameter))
            throw new IllegalArgumentException("Unknown parameter: " + parameter);
        int original = pathParamsOrder.get(parameter);
        if (original == index)
            return this;
        String previous = reversePathParamsOrder.get(index);
        pathParamsOrder.put(previous, original);
        reversePathParamsOrder.put(original, previous);
        pathParamsOrder.put(parameter, index);
        reversePathParamsOrder.put(index, parameter);
        return this;
    }

    public Route use(HttpHandler handler) {
        this.direct = handler;
        return this;
    }

    public Route use(String methodPath) {
        return use(methodPath, null);
    }

    public Route use(Method method) {
        return use(method, null);
    }

    public Route use(String methodPath, Object parent) {
        try {
            return use(RoutePath.of(methodPath, RoutePath.parameterCount(path)), parent);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to find target method.", e);
        }
    }

    public Route use(Method method, Object parent) {
        this.method = method;
        this.parent = parent;
        if (method != null && !HttpHandler.class.isAssignableFrom(method.getReturnType()))
            throw new RuntimeException("Routes must return a HttpHandler. Actual: " + method.getReturnType());
        return this;
    }

    public boolean matches(String uri) {
        if (null == uri)
            throw new IllegalArgumentException();
        return (evaluatedPattern != null ? evaluatedPattern : (evaluatedPattern = compile())).matcher(uri).matches();
    }

    @SuppressWarnings("non-varargs")
    public Object invoke(HttpRequest req) throws InvocationTargetException, IllegalAccessException {
        Set<Object> valueSet = new HashSet<>();
        if (direct != null)
            return direct;
        if (null == method)
            throw new RuntimeException("No method configured for route. Route#use must be called to assign the method to invoke.");
        method.setAccessible(true);
        Matcher matcher = (evaluatedPattern != null ? evaluatedPattern : (evaluatedPattern = compile())).matcher(req.uri());
        if (!matcher.matches())
            throw new RuntimeException("Unable to match uri against route pattern.");
        if (matcher.groupCount() != pathParams.size())
            throw new RuntimeException("Parameter mismatch. Unable to find matcher group for each argument.");
        //没有入参
        if (pathParams.isEmpty() && method.getParameterCount() == 0) {
            return method.invoke(parent, null);
        }

        // path prames
        Class<?>[] types = method.getParameterTypes();
        for (int i = 0; i < pathParams.size(); i++) { // 路径上的入参,追加到 req.params
            String s = matcher.group(i + 1);
            Class<?> c = types[i];
            req.params().putIfAbsent(reversePathParamsOrder.get(i), getValOfBaseType(c, s));
        }

        int methodParmsNums = method.getParameterCount();
        if (methodParmsNums != req.params().size()) {
            logger.error("Parameters size mismatch.methodParmsNums:{}, Request params:{}",methodParmsNums,req.params().size());
            throw new RuntimeException(" Parameters size mismatch.");
        }

        return method.invoke(parent, getDefParamArr(types, req.params()));

    }

    private Object[] getDefParamArr(Class<?>[] types, Map<String, Object> params) {
        Object[] tArr = new Object[params.size()];
        int i = 0;
        for (Object key : params.keySet()) {
            tArr[i] = getValOfBaseType(types[i], (String) params.get(key));
            i++;
        }
        return tArr;
    }

    private Object getValOfBaseType(Class<?> c, String v) {
        if (c == int.class || c == Integer.class)
            return Integer.parseInt(v);
        else if (c == long.class || c == Long.class)
            return Long.parseLong(v);
        else if (c == float.class || c == Float.class)
            return Float.parseFloat(v);
        else if (c == double.class || c == Double.class) {
            return Double.parseDouble(v);
        }
        else if (c == Date.class) {
            return DateTime.parse(v).toDate();
        }
        else if (c == DateTime.class) {
            return DateTime.parse(v);
        }
        return v;
    }

    public Pattern compile() {
        StringBuilder builder = new StringBuilder();
        int lastStart;
        int lastEnd = 0;
        while ((lastStart = path.indexOf('{', lastEnd)) != -1) {
            if (lastStart == 0)
                throw new RuntimeException("Missing beginning / in route path: " + path);
            if (lastStart == path.length() - 1)
                throw new RuntimeException("Malformed route path. Unclosed parameter name: " + path);
            if (lastStart != lastEnd + 1)
                builder.append(Pattern.quote(path.substring(lastEnd, lastStart)));
            int closingIndex = path.indexOf('}', lastStart);
            String pattern = parameterPatterns.get(path.substring(lastStart + 1, closingIndex));
            if (!pattern.startsWith("(") && !pattern.endsWith(")"))
                builder.append("(").append(pattern).append(")");
            else
                builder.append(pattern);
            lastEnd = closingIndex;
        }
        if (lastEnd == 0)
            builder.append(path);
        return Pattern.compile(builder.toString());
    }
}
