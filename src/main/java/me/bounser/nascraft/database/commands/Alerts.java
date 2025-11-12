package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.database.commands.resources.NormalisedDate;
import me.bounser.nascraft.discord.alerts.DiscordAlerts;
import me.bounser.nascraft.market.unit.Item;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;

import static me.biquaternions.nascraft.schema.public_.Tables.ALERTS;

public class Alerts {

    public static void addAlert(DSLContext dsl, String userid, Item item, double price) {
        try {
            dsl.insertInto(ALERTS)
                    .set(ALERTS.DAY, NormalisedDate.getDays())
                    .set(ALERTS.USERID, userid)
                    .set(ALERTS.IDENTIFIER, item.getIdentifier())
                    .set(ALERTS.PRICE, price)
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }

    public static void removeAlert(DSLContext dsl, String userid, Item item) {
        try {
            dsl.deleteFrom(ALERTS)
                    .where(ALERTS.USERID.eq(userid))
                    .and(ALERTS.IDENTIFIER.eq(item.getIdentifier()))
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }

    public static void retrieveAlerts(DSLContext dsl) {
        try {
            var result = dsl.select(ALERTS.USERID, ALERTS.IDENTIFIER, ALERTS.PRICE)
                    .from(ALERTS)
                    .fetch();

            for (var record : result) {
                DiscordAlerts.getInstance().setAlert(record.getValue(ALERTS.USERID), record.getValue(ALERTS.IDENTIFIER), record.getValue(ALERTS.PRICE));
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }

    public static void removeAllAlerts(DSLContext dsl, String userId) {
        try {
            dsl.deleteFrom(ALERTS)
                    .where(ALERTS.USERID.eq(userId))
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }

    public static void purgeAlerts(DSLContext dsl) {
        int expiration = Config.getInstance().getAlertsDaysUntilExpired();
        int days = NormalisedDate.getDays();
        try {
            dsl.deleteFrom(ALERTS)
                    .where(ALERTS.DAY.lessThan(days - expiration))
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }
}
