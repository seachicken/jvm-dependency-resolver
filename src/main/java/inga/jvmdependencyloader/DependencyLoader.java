package inga.jvmdependencyloader;

import inga.jvmdependencyloader.buildtool.BuildTool;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class DependencyLoader implements AutoCloseable {
    private final Map<Path, URLClassLoader> classLoaders = new HashMap<>();

    public List<Method> readMethods(String fqcn, Path from) {
        try (URLClassLoader classLoader = loadClassLoader(from)) {
            if (classLoader == null) {
                System.err.println("classLoader is not found. from: " + from);
                return Collections.emptyList();
            }

            var methods = classLoader.loadClass(fqcn).getMethods();
            return Arrays.stream(methods)
                    .map(Method::new)
                    .collect(Collectors.toMap(
                            (m) -> m.name() + m.parameterTypes(),
                            (m) -> m,
                            (a, b) -> a.returnType().isInterface() ? b : a
                    ))
                    .values().stream().toList();
        } catch (ClassNotFoundException | NoClassDefFoundError | IOException e) {
            e.printStackTrace(System.err);
            return Collections.emptyList();
        }
    }

    public List<Clazz> readClasses(String fqcn, Path from) {
        try (URLClassLoader classLoader = loadClassLoader(from)) {
            if (classLoader == null) {
                System.err.println("classLoader is not found. from: " + from);
                return Collections.emptyList();
            }

            var classes = classLoader.loadClass(fqcn).getDeclaredClasses();
            return Arrays.stream(classes)
                    .map(c -> new Clazz(c.getName()))
                    .collect(Collectors.toList());
        } catch (ClassNotFoundException | NoClassDefFoundError | IOException e) {
            e.printStackTrace(System.err);
            return Collections.emptyList();
        }
    }

    public List<Type> readHierarchy(String fqcn, Path from) {
        try (URLClassLoader classLoader = loadClassLoader(from)) {
            if (classLoader == null) {
                System.err.println("classLoader is not found. from: " + from);
                return Collections.emptyList();
            }

            var results = new ArrayList<Type>();
            var stack = new Stack<Class<?>>();
            stack.push(classLoader.loadClass(fqcn));
            while (!stack.isEmpty()) {
                var clazz = stack.pop();
                results.add(new Type(clazz));
                var parents = new ArrayList<>(List.of(clazz.getInterfaces()));
                if (clazz.getSuperclass() != null) {
                    parents.add(clazz.getSuperclass());
                }
                for (var parent : parents) {
                    if (results.stream().noneMatch(t -> t.name().equals(parent.getName()))
                            && !stack.contains(parent)) {
                        stack.push(parent);
                    }
                }
            }
            Collections.reverse(results);
            return results;
        } catch (ClassNotFoundException | NoClassDefFoundError | IOException e) {
            e.printStackTrace(System.err);
            return Collections.emptyList();
        }
    }

    private URLClassLoader loadClassLoader(Path from) {
        if (from == null) {
            return null;
        }
        URLClassLoader classLoader;
        if (classLoaders.containsKey(from)) {
            classLoader = classLoaders.get(from);
        } else {
            classLoader = BuildTool.create(from).load();
            classLoaders.put(from, classLoader);
        }
        return classLoader;
    }

    @Override
    public void close() throws Exception {
        for (var loader : classLoaders.values()) {
            loader.close();
        }
    }
}
