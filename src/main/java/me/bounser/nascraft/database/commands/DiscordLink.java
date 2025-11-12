package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.Nascraft;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;

import java.util.UUID;
import java.util.logging.Level;

import static me.biquaternions.nascraft.schema.public_.Tables.DISCORD_LINKS;

public class DiscordLink {

    public static void saveLink(DSLContext dsl, String userId, UUID uuid, String nickname) {
        try {
            dsl.insertInto(DISCORD_LINKS)
                    .set(DISCORD_LINKS.USERID, userId)
                    .set(DISCORD_LINKS.UUID, uuid.toString())
                    .set(DISCORD_LINKS.NICKNAME, nickname)
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getLogger().log(Level.WARNING, e.getMessage());
        }
    }

    public static void removeLink(DSLContext dsl, String userId) {
        try {
            dsl.deleteFrom(DISCORD_LINKS)
                    .where(DISCORD_LINKS.USERID.eq(userId))
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getLogger().log(Level.WARNING, e.getMessage());
        }
    }

    public static UUID getUUID(DSLContext dsl, String userId) {
        try {
            Record record = dsl.select(DISCORD_LINKS.UUID)
                    .from(DISCORD_LINKS)
                    .where(DISCORD_LINKS.USERID.eq(userId))
                    .fetchOne();

            if (record != null) {
                return UUID.fromString(record.getValue(DISCORD_LINKS.UUID));
            }

        } catch (DataAccessException e) {
            Nascraft.getInstance().getLogger().log(Level.WARNING, e.getMessage());
        }

        return null;
    }

    public static String getNickname(DSLContext dsl, String userId) {
        try {
            Record record = dsl.select(DISCORD_LINKS.NICKNAME)
                    .from(DISCORD_LINKS)
                    .where(DISCORD_LINKS.USERID.eq(userId))
                    .fetchOne();

            if (record != null) {
                return record.getValue(DISCORD_LINKS.NICKNAME);
            }

        } catch (DataAccessException e) {
            Nascraft.getInstance().getLogger().log(Level.WARNING, e.getMessage());
        }

        return null;
    }

    public static String getUserId(DSLContext dsl, UUID uuid) {
        try {
            Record record = dsl.select(DISCORD_LINKS.USERID)
                    .from(DISCORD_LINKS)
                    .where(DISCORD_LINKS.UUID.eq(uuid.toString()))
                    .fetchOne();

            if (record != null) {
                return record.getValue(DISCORD_LINKS.USERID);
            }

        } catch (DataAccessException e) {
            Nascraft.getInstance().getLogger().log(Level.WARNING, e.getMessage());
        }

        return null;
    }

}
