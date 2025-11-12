package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.Nascraft;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;

import java.util.UUID;

import static me.biquaternions.nascraft.schema.public_.Tables.USER_NAMES;

public class UserNames {

    public static String getNameByUUID(DSLContext dsl, UUID uuid) {
        try {
            var record = dsl.select(USER_NAMES.NAME)
                    .from(USER_NAMES)
                    .where(USER_NAMES.UUID.eq(uuid.toString()))
                    .fetchOne();

            if (record != null) {
                return record.get(USER_NAMES.NAME);
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return null;
    }

    public static void saveOrUpdateNick(DSLContext dsl, UUID uuid, String name) {
        try {
            dsl.insertInto(USER_NAMES)
                    .set(USER_NAMES.UUID, uuid.toString())
                    .set(USER_NAMES.NAME, name)
                    .onDuplicateKeyUpdate()
                    .set(USER_NAMES.NAME, name)
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }

    public static UUID getUUIDbyName(DSLContext dsl, String name) {
        try {
            var record = dsl.select(USER_NAMES.UUID)
                    .from(USER_NAMES)
                    .where(USER_NAMES.NAME.eq(name))
                    .fetchOne();

            if (record != null) {
                return UUID.fromString(record.get(USER_NAMES.UUID));
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return null;
    }
}
