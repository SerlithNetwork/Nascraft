package me.bounser.nascraft.managers;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MetadataManager {

    private static final ConcurrentMap<UUID, String> NASCRAFT_MENU = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, Integer> NASCRAFT_MENU_PAGE = new ConcurrentHashMap<>();

    private static final ConcurrentMap<UUID, String> NASCRAFT_LOG_INVENTORY = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, Integer> NASCRAFT_LOG_INVENTORY_PAGE = new ConcurrentHashMap<>();

}
