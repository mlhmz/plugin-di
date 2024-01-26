package xyz.mlhmz.plugindi;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.reflections.Reflections;
import xyz.mlhmz.plugindi.annotations.RegisterCommand;
import xyz.mlhmz.plugindi.annotations.RegisterListener;
import xyz.mlhmz.plugindi.exceptions.UnknownAnnotationType;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import static org.reflections.scanners.Scanners.SubTypes;
import static org.reflections.scanners.Scanners.TypesAnnotated;

public final class PluginDI {
    private static Reflections reflections;

    private PluginDI() {}

    public static void init(JavaPlugin plugin) {
        reflections = new Reflections("xyz.mlhmz.plugindi");
        Set<Class<?>> commandClasses = getAnnotatedClassesOf(RegisterCommand.class);
        Set<Class<?>> listenerClasses = getAnnotatedClassesOf(RegisterListener.class);

        Set<CommandExecutor> commands = instantiateSet(commandClasses, CommandExecutor.class);
        Set<Listener> listeners = instantiateSet(listenerClasses, Listener.class);

        Map<String, Set<CommandExecutor>> commandModules = sortSetByModule(commands);
        Map<String, Set<Listener>> listenerModules = sortSetByModule(listeners);

        registerCommands(plugin, commandModules);

        registerListeners(plugin, listenerModules);
    }

    private static void registerCommands(JavaPlugin plugin, Map<String, Set<CommandExecutor>> commandModules) {
        commandModules.entrySet()
                .stream()
                .filter(entry -> plugin.getConfig().getBoolean(String.format("modules.%s", entry.getKey()), false))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .forEach(command ->
                        Objects.requireNonNull(
                                plugin.getCommand(
                                        command.getClass().getAnnotation(RegisterCommand.class).value()
                                )
                        ).setExecutor(command)
                );
    }

    private static void registerListeners(JavaPlugin plugin, Map<String, Set<Listener>> listenerModules) {
        listenerModules.entrySet()
                .stream()
                .filter(entry -> plugin.getConfig().getBoolean(String.format("modules.%s", entry.getKey()), false))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .forEach(listener -> Bukkit.getPluginManager().registerEvents(listener, plugin));
    }

    private static <T> Map<String, Set<T>> sortSetByModule(Set<T> instances) {
        Map<String, Set<T>> executors = new HashMap<>();
        for (T object : instances) {
            String module = getModuleByAnnotationType(object);
            Set<T> set = executors.computeIfAbsent(module, k -> new HashSet<>());
            set.add(object);
            executors.put(module, set);
        }
        return executors;
    }

    private static <T> String getModuleByAnnotationType(T object) {
        if (object instanceof CommandExecutor) {
            CommandExecutor executor = (CommandExecutor) object;
            RegisterCommand annotation = executor.getClass().getAnnotation(RegisterCommand.class);
            return annotation.module();
        } else if (object instanceof Listener) {
            Listener executor = (Listener) object;
            RegisterListener annotation = executor.getClass().getAnnotation(RegisterListener.class);
            return annotation.module();
        } else {
            throw new UnknownAnnotationType(object);
        }
    }

    private static <T> Set<T> instantiateSet(Set<Class<?>> commands, Class<T> clazz) {
        return commands.stream()
                .filter(command -> command.getInterfaces()[0].equals(clazz))
                .map(PluginDI::instantiateClass)
                .map(clazz::cast)
                .collect(Collectors.toSet());
    }

    private static Object instantiateClass(Class<?> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static Set<Class<?>> getAnnotatedClassesOf(Class<?> annotation) {
        return reflections.get(SubTypes.of(TypesAnnotated.of(annotation)).asClass());
    }
}
