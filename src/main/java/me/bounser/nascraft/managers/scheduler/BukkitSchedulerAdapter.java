package me.bounser.nascraft.managers.scheduler;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Implementation of SchedulerAdapter for traditional Bukkit servers.
 */
public class BukkitSchedulerAdapter implements SchedulerAdapter {

    private final Nascraft plugin;
    private final ExecutorService executor;

    public BukkitSchedulerAdapter(Nascraft plugin) {
        this.plugin = plugin;

        Config config = Config.getInstance();
        this.executor = Executors.newFixedThreadPool(
                config.getAsyncThreads(),
                new ThreadFactoryBuilder()
                        .setNameFormat("Nascraft Task Executor Thread - %d")
                        .setPriority(Thread.NORM_PRIORITY - 3)
                        .build()
        );
    }

    @Override
    public CompletableFuture<Void> runAsync(Runnable task) {
        final Throwable callerStackTrace = new Throwable();
        callerStackTrace.setStackTrace(Thread.currentThread().getStackTrace());
        return CompletableFuture.runAsync(task, this.executor)
                .exceptionally(ex -> {
                    Nascraft.getInstance().getSLF4JLogger().error(ex.getMessage(), ex);
                    Nascraft.getInstance().getSLF4JLogger().error("Scheduler stacktrace", callerStackTrace);
                    return null;
                });
    }

    @Override
    public <T> CompletableFuture<T> runAsync(Supplier<T> task) {
        final Throwable callerStackTrace = new Throwable();
        callerStackTrace.setStackTrace(Thread.currentThread().getStackTrace());
        return CompletableFuture.supplyAsync(task, this.executor)
                .exceptionally(ex -> {
                    Nascraft.getInstance().getSLF4JLogger().error(ex.getMessage(), ex);
                    Nascraft.getInstance().getSLF4JLogger().error("Scheduler stacktrace", callerStackTrace);
                    return null;
                });
    }

    @Override
    public CompletableFuture<Void> runGlobal(Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (Bukkit.isPrimaryThread()) {
            try {
                task.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    task.run();
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        }
        return future;
    }

    @Override
    public CompletableFuture<Void> runForEntity(Entity entity, Consumer<Entity> task) {
        // In Bukkit, all entities are on the main thread, so we just run on the main thread
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (Bukkit.isPrimaryThread()) {
            try {
                task.accept(entity);
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    task.accept(entity);
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        }
        return future;
    }

    @Override
    public CompletableFuture<Void> runAtLocation(Location location, Consumer<Location> task) {
        // In Bukkit, all locations are on the main thread, so we just run on the main thread
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (Bukkit.isPrimaryThread()) {
            try {
                task.accept(location);
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    task.accept(location);
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        }
        return future;
    }

    @Override
    public int scheduleAsyncRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, initialDelayTicks, periodTicks).getTaskId();
    }

    @Override
    public int scheduleGlobalRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, initialDelayTicks, periodTicks).getTaskId();
    }

    @Override
    public void cancelTask(int taskId) {
        Bukkit.getScheduler().cancelTask(taskId);
    }

    @Override
    public boolean isMainThread() {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isFolia() {
        return false;
    }

    @Override
    public void shutdown() {
        this.executor.shutdown();
        try {
            if (this.executor.awaitTermination(5, TimeUnit.SECONDS)) {
                this.executor.shutdownNow();
            }
        } catch (InterruptedException ignore) {}
    }

} 