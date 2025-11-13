package me.bounser.nascraft.network;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import me.bounser.nascraft.inventorygui.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NascraftPacketListener implements PacketListener {

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            Player player = event.getPlayer();
            if (!player.hasMetadata("NascraftMenu")) {
                return;
            }
            event.setCancelled(true);
            this.handleInventoryClickPacket(new WrapperPlayClientClickWindow(event), player);
        }
    }

    public void handleInventoryClickPacket(@NotNull WrapperPlayClientClickWindow packet, Player player) {
    }
}
