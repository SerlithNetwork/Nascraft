package me.bounser.nascraft.discord.alerts;

import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.discord.DiscordBot;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DiscordAlerts implements Listener {

    // UserId -> (Item, Price)
    private final ConcurrentMap<String, ConcurrentMap<Item, Double>> alerts = new ConcurrentHashMap<>();

    private static DiscordAlerts instance;

    public static DiscordAlerts getInstance() {
        if (instance == null) {
            instance = new DiscordAlerts();

            DatabaseManager.get().getDatabase().purgeAlerts();
            DatabaseManager.get().getDatabase().retrieveAlerts();
        }
        return instance;
    }

    public OperationResult setAlert(String userID, String identifier, double price) {

        Item item = MarketManager.getInstance().getItem(identifier);

        if (item == null) return OperationResult.NOT_VALID;

        if (alerts.containsKey(userID) && alerts.get(userID).size() > 8) return OperationResult.LIMIT_REACHED;

        if (alerts.containsKey(userID) && alerts.get(userID).containsKey(item)) return OperationResult.REPEATED;

        ConcurrentMap<Item, Double> content;
        if (alerts.get(userID) == null) content = new ConcurrentHashMap<>();
        else content = alerts.get(userID);

        if (price < item.getPrice().getValue()) content.put(item, -price);
        else content.put(item, price);

        alerts.put(userID, content);
        DatabaseManager.get().getDatabase().addAlert(userID, item, price);

        return OperationResult.SUCCESS;
    }

    public OperationResult removeAlert(String userID, Item item) {

        ConcurrentMap<Item, Double> content = alerts.get(userID);
        if (content == null || !content.containsKey(item)) {
            return OperationResult.NOT_FOUND;
        }

        content.remove(item);
        DatabaseManager.get().getDatabase().removeAlert(userID, item);
        return OperationResult.SUCCESS;
    }

    public void updateAlerts() {

        ConcurrentMap<String, Item> alertsToRemove = new ConcurrentHashMap<>();

        for (String userID : alerts.keySet()) {

            for (Item item : alerts.get(userID).keySet()) {

                if (alerts.get(userID).get(item) < 0) {

                    if (!(item.getPrice().getValue() < Math.abs(alerts.get(userID).get(item)))) continue;

                    reachedMessage(userID, item, Math.abs(alerts.get(userID).get(item)), ":chart_with_downwards_trend:", false);

                } else {

                    if (!(item.getPrice().getValue() > alerts.get(userID).get(item))) continue;

                    reachedMessage(userID, item, Math.abs(alerts.get(userID).get(item)), ":chart_with_upwards_trend:", true);
                }
                alertsToRemove.put(userID, item);
            }
        }

        for (String userid : alertsToRemove.keySet()) {
            removeAlert(userid, alertsToRemove.get(userid));
        }
    }

    public ConcurrentMap<String, ConcurrentMap<Item, Double>> getAlerts() {
        return alerts;
    }

    public ConcurrentMap<Item, Double> getAlertsOfUUID(UUID uuid) {
        String userid = LinkManager.getInstance().getUserDiscordID(uuid);
        if (userid == null) {
            return null;
        }
        return alerts.get(userid);
    }

    public void reachedMessage(String userId, Item item, double price, String emoji, boolean up) {

        Player player = Bukkit.getPlayer(LinkManager.getInstance().getUUID(userId));

        if (player != null) {
            Message message = up ? Message.ALERT_REACHED_UP : Message.ALERT_REACHED_DOWN;

            Lang.get().message(player, Lang.get().message(message)
                    .replace("[NAME]", item.getTaggedName())
                    .replace("[PRICE1]", Formatter.format(item.getCurrency(), price, Style.ROUND_BASIC))
                    .replace("[PRICE2]", Formatter.format(item.getCurrency(), item.getPrice().getValue(), Style.ROUND_BASIC)));
        }

        DiscordBot.getInstance().getJDA().retrieveUserById(userId).queue(user ->
                user.openPrivateChannel()
                        .queue(privateChannel -> privateChannel
                                .sendMessage(Lang.get().message(Message.DISCORD_ALERT_REACHED_SEGMENT)
                                        .replace("[EMOJI]", emoji)
                                        .replace("[NAME]", item.getName())
                                        .replace("[PRICE1]", Formatter.plainFormat(item.getCurrency(), price, Style.ROUND_BASIC))
                                        .replace("[PRICE2]", Formatter.plainFormat(item.getCurrency(), item.getPrice().getValue(), Style.ROUND_BASIC)))
                                .queue()));

    }

}
