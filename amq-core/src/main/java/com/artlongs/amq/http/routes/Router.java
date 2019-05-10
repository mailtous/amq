package com.artlongs.amq.http.routes;

import com.artlongs.amq.http.HttpHandler;
import com.artlongs.amq.http.HttpRequest;
import com.artlongs.amq.http.HttpResponse;
import com.artlongs.amq.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Stream;

/**
 * Router 路由集合
 * <p>
 * An object that routes incoming HTTP requests to an appropriate route for response.
 * <p>
 * Router is thread-safe, making use of thread-locals to ensure the selected {@code Route}
 * when visiting the request write is consistent until when {@code Router#response(HttpWriter)} is called.
 *
 * @author leeton
 * @since 1.0
 */
public class Router implements HttpHandler {
    private static Logger logger = LoggerFactory.getLogger(Router.class);
    private static final Map<String, List<Route>> routes = new HashMap<>();

    /**
     * Constructs a new {@code Router} converting the methods annotated with {@code Get}, {@code Post}, and {@code Delete} to routes.
     * <p>
     * A method subject to becoming a {@code Route} must be annotated with {@code Get}, {@code Post}, or {@code Delete}.
     * <p>
     * The method return type must be a {@code HttpHandler}.
     * <p>
     * Each class is restricted to containing static routes.
     * <p>
     * Use {@code Router.asRouter(Controller...)} for already instantiated controllers.
     *
     * @param classes the classes containing annotated route methods
     * @return a new {@code Router}
     */
    public static Router asRouter(Class<? extends Controller>... classes) {
        if (null == classes)
            throw new IllegalArgumentException();
        Router router = new Router();
        for (Class<? extends Controller> c : classes)
            Router.addRoutes(c, router);
        return router;
    }

    /**
     * Constructs a new {@code Router} converting the methods annotated with {@code Get}, {@code Post}, and {@code Delete} to routes.
     * <p>
     * A method subject to becoming a {@code Route} must be annotated with {@code Get}, {@code Post}, or {@code Delete}.
     * <p>
     * The method return type must be a {@code HttpHandler}.
     *
     * @param controllers the controllers containing annotated route methods
     * @return a new {@code Router}
     */
    public static Router asRouter(Controller... controllers) {
        if (null == controllers) throw new IllegalArgumentException();
        Router router = new Router();
        for (Controller controller : controllers) {
            Router.addRoutes(controller.getClass(), controller, router);
        }
        return router;
    }

    /**
     * Converts the methods annotated with {@code Get}, {@code Post}, and {@code Delete} to routes and adds them to the specified router.
     * <p>
     * A method subject to becoming a {@code Route} must be annotated with {@code Get}, {@code Post}, or {@code Delete}.
     * <p>
     * The method return type must be a {@code HttpHandler}.
     * <p>
     * Each class is restricted to containing static routes.
     * <p>
     * Use {@code Router.addRoutes(Controller, Router)} for already instantiated controllers.
     *
     * @param c      the controller class
     * @param router the target router
     */
    public static void addRoutes(Class<? extends Controller> c, Router router) {
        Router.addRoutes(c, null, router);
    }

    /**
     * Converts the methods annotated with {@code Get}, {@code Post}, and {@code Delete} to routes and adds them to the specified router.
     * <p>
     * A method subject to becoming a {@code Route} must be annotated with {@code Get}, {@code Post}, or {@code Delete}.
     * <p>
     * The method return type must be a {@code HttpHandler}.
     *
     * @param controller the controller instance (if there are instance methods representing routes)
     * @param router     the target router
     */
    public static void addRoutes(Controller controller, Router router) {
        if (null == controller)
            throw new IllegalArgumentException();
        Router.addRoutes(controller.getClass(), controller, router);
    }

    /**
     * Converts the methods annotated with {@code Get}, {@code Post}, and {@code Delete} to routes and adds them to the specified router.
     * <p>
     * A method subject to becoming a {@code Route} must be annotated with {@code Get}, {@code Post}, or {@code Delete}.
     * <p>
     * The method return type must be a {@code HttpHandler}.
     *
     * @param controllerClass the controller class
     * @param controller      the controller instance (if there are instance methods representing routes)
     * @param router          the target router
     */
    private static void addRoutes(Class<? extends Controller> controllerClass, Controller controller, Router router) {
        if (null == controllerClass || null == router)
            throw new IllegalArgumentException();
        Class c = controllerClass;
        do {
            Stream.concat(Arrays.stream(c.getMethods()), Arrays.stream(c.getDeclaredMethods())).forEach(method -> {
                String requestType;
                String path;
                String[] patterns;
                List<Parameter> methodParams = new ArrayList();
                int[] indexes;
                if (method.isAnnotationPresent(Get.class)) {
                    Get get = method.getAnnotation(Get.class);
                    requestType = HttpRequest.METHOD_GET;
                    path = get.value();
                    patterns = get.patterns();
                    indexes = get.indexes();
                } else if (method.isAnnotationPresent(Post.class)) {
                    Post post = method.getAnnotation(Post.class);
                    requestType = HttpRequest.METHOD_POST;
                    path = post.value();
                    patterns = post.patterns();
                    indexes = post.indexes();
                    methodParams = Arrays.asList(method.getParameters());
                } else {
                    return;
                }
                List<String> parameters = RoutePath.parameters(path);
                parameters = RoutePath.addMethodParms(parameters, methodParams);
                if (patterns.length != 0 && patterns.length != parameters.size())
                    throw new IllegalArgumentException("Parameter mismatch. A pattern must be specified for all parameters, if any.");
                if (indexes.length != 0 && indexes.length != parameters.size())
                    throw new IllegalArgumentException("Parameter mismatch. An index must be specified for all parameters, if any.");
                Route route = new Route(requestType, path);
                for (int i = 0; i < patterns.length; i++) {
                    route.where(parameters.get(i), patterns[i]);
                }
                for (int i = 0; i < indexes.length; i++) {
                    route.where(parameters.get(i), indexes[i]);
                }
                boolean isStatic = Modifier.isStatic(method.getModifiers());
                if (!isStatic && null == controller)
                    throw new IllegalArgumentException("Illegal route. Methods must be declared static for non-instantiated controllers.");
                route.use(method, isStatic ? null : controller);
                router.add(route);
            });
        } while ((c = c.getSuperclass()) != null);
    }

    /**
     * Creates a new {@code Route} with a "GET" request type and the specified path.
     * <p>
     * The method returns the newly created route to allow customizing the routes parameter ordering and patterns.
     *
     * @param path the route path
     * @return the newly created route
     */
    public Route get(String path) {
        Route route = Route.get(path);
        add(route);
        return route;
    }

    /**
     * Creates a new {@code Route} with a "POST" request type and the specified path.
     * <p>
     * The method returns the newly created route to allow customizing the routes parameter ordering and patterns.
     *
     * @param path the route path
     * @return the newly created route
     */
    public Route post(String path) {
        Route route = Route.post(path);
        add(route);
        return route;
    }

    /**
     * Creates a new {@code Route} with a "DELETE" request type and the specified path.
     * <p>
     * The method returns the newly created route to allow customizing the routes parameter ordering and patterns.
     *
     * @param path the route path
     * @return the newly created route
     */
    public Route delete(String path) {
        Route route = Route.delete(path);
        add(route);
        return route;
    }

    /**
     * Adds a new route to the router.
     *
     * @param route the route
     * @return {@code this} for method-chaining
     */
    public Router add(Route route) {
        if (null == route) throw new IllegalArgumentException();

        String requestType = route.requestType(); // GET/POST ...
        if (routes.containsKey(requestType)) {
            List<Route> routeList = routes.get(requestType);
            boolean find = false;
            for (Route r : routeList) {
                if(r.path().equals(route.path())){
                    find = true;
                    break;
                }
            }
            if(!find){
                routeList.add(route);
            }

        } else {
            List<Route> routes = new ArrayList<>();
            routes.add(route);
            this.routes.put(requestType, routes);
        }
        return this;
    }

    /**
     * Locates the route that matches the incoming request uri.
     * <p>
     * A route must be an absolute match to the request uri to be considered.
     *
     * @param uri the uri
     * @return the best route that matched the uri
     */
    public Route find(String requestType, String uri) {
        if (!routes.containsKey(requestType))
            throw new RuntimeException("Unable to find route. Request type: " + requestType + ", uri: " + uri);
        List<Route> routes = this.routes.get(requestType);
        for (Route route : routes) {
            if (route.matches(uri)) {
                return route;
            }
        }
        logger.error("Unable to find route. Request type: " + requestType + ", uri: " + uri);
        return null;
    }

    @Override
    public void handle(HttpRequest req, HttpResponse res) {
        if (null == req || null == res) return;
        try {
            Object o = find(req.method(), req.uri());
            if (o!=null && !((Route) o).path().endsWith("404")) {
                if (null != o) o = ((Route) o).invoke(req);
                if (o instanceof HttpHandler) {
                    ((HttpHandler) o).handle(req, res);
                }
            } else {// 404
                HttpHandler e404 = new HttpHandler() {
                    @Override
                    public void handle(HttpRequest req, HttpResponse resp) {
                        resp.setState(HttpStatus.NOT_FOUND);
                        resp.append("<h2>Page Not Found. (404)</h2>");
                        resp.end();
                    }
                };
                e404.handle(req, res);
            }

        } catch (InvocationTargetException | IllegalAccessException e) {
            // throw new RuntimeException("Unable to invoke route action.", e);
            logger.error("Unable to invoke route action.", e);
        }
    }


}
