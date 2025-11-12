package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.Nascraft;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;

import java.util.UUID;

import static me.biquaternions.nascraft.schema.public_.Tables.DISCORD;

public class Discord {

    public static void saveDiscordLink(DSLContext dsl, UUID uuid, String userId, String nickname) {
        try {
            dsl.insertInto(DISCORD)
                    .set(DISCORD.USERID, userId)
                    .set(DISCORD.NICKNAME, nickname)
                    .set(DISCORD.UUID, uuid.toString())
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }

    public static void removeLink(DSLContext dsl, UUID uuid) {
        try {
            dsl.deleteFrom(DISCORD)
                    .where(DISCORD.UUID.eq(uuid.toString()))
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }

    public static UUID getUUIDFromUserid(DSLContext dsl, String userId) {
        try {
            var record = dsl.select(DISCORD.UUID)
                    .from(DISCORD)
                    .where(DISCORD.USERID.eq(userId))
                    .fetchOne();

            if (record != null) {
                return UUID.fromString(record.getValue(DISCORD.UUID));
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return null;
    }

    public static String getDiscordUserId(DSLContext dsl, UUID uuid) {
        try {
            var record = dsl.select(DISCORD.USERID)
                    .from(DISCORD)
                    .where(DISCORD.UUID.eq(uuid.toString()))
                    .fetchOne();

            if (record != null) {
                return record.getValue(DISCORD.USERID);
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return null;
    }

    public static String getNicknameFromUserId(DSLContext dsl, String userId) {
        try {
            var record = dsl.select(DISCORD.NICKNAME)
                    .from(DISCORD)
                    .where(DISCORD.USERID.eq(userId))
                    .fetchOne();

            if (record != null) {
                return record.getValue(DISCORD.NICKNAME);
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return null;
    }

}
