package me.bounser.nascraft.managers;

import me.bounser.nascraft.managers.scheduler.SchedulerAdapter;
import me.bounser.nascraft.managers.scheduler.SchedulerManager;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class AsyncManager {

    private static final Set<UUID> TASKS = ConcurrentHashMap.newKeySet();
    private static final SchedulerAdapter SCHEDULER = SchedulerManager.getInstance();

    public static CompletableFuture<Void> executeAsync(Player player, Runnable task) {
        TASKS.add(player.getUniqueId());
        return SCHEDULER.runAsync(task)
                .whenComplete((result, ex) -> TASKS.remove(player.getUniqueId()));
    }

    public static <T> CompletableFuture<T> submitAsync(Player player, Supplier<T> task) {
        TASKS.add(player.getUniqueId());
        return SCHEDULER.runAsync(task)
                .whenComplete((result, ex) -> TASKS.remove(player.getUniqueId()));
    }

    public static boolean hasTaskRunning(Player player) {
        return TASKS.contains(player.getUniqueId());
    }

}
