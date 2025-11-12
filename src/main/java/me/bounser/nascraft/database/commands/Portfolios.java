package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;

import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.logging.Level;

import static me.biquaternions.nascraft.schema.public_.Tables.CAPACITIES;
import static me.biquaternions.nascraft.schema.public_.Tables.PORTFOLIOS;

public class Portfolios {

    public static void updateItemPortfolio(DSLContext dsl, UUID uuid, Item item, int quantity) {
        try {
            dsl.insertInto(PORTFOLIOS)
                    .set(PORTFOLIOS.UUID, uuid.toString())
                    .set(PORTFOLIOS.IDENTIFIER, item.getIdentifier())
                    .onDuplicateKeyUpdate()
                    .set(PORTFOLIOS.AMOUNT, quantity)
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getLogger().log(Level.WARNING, e.getMessage(), e);
        }
    }

    public static void removeItemPortfolio(DSLContext dsl, UUID uuid, Item item) {
        try {
            dsl.deleteFrom(PORTFOLIOS)
                    .where(PORTFOLIOS.UUID.eq(uuid.toString()))
                    .and(PORTFOLIOS.IDENTIFIER.eq(item.getIdentifier()))
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getLogger().log(Level.WARNING, e.getMessage(), e);
        }
    }

    public static void clearPortfolio(DSLContext dsl, UUID uuid) {
        try {
            dsl.deleteFrom(PORTFOLIOS)
                    .where(PORTFOLIOS.UUID.eq(uuid.toString()))
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getLogger().log(Level.WARNING, e.getMessage(), e);
        }
    }

    public static void updateCapacity(DSLContext dsl, UUID uuid, int capacity) {
        try {
            dsl.update(CAPACITIES)
                    .set(CAPACITIES.CAPACITY, capacity)
                    .where(CAPACITIES.UUID.eq(uuid.toString()))
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getLogger().log(Level.WARNING, e.getMessage(), e);
        }
    }

    public static LinkedHashMap<Item, Integer> retrievePortfolio(DSLContext dsl, UUID uuid) {
        LinkedHashMap<Item, Integer> content = new LinkedHashMap<>();
        try {
            var result = dsl.select(PORTFOLIOS.IDENTIFIER, PORTFOLIOS.AMOUNT)
                    .from(PORTFOLIOS)
                    .where(PORTFOLIOS.UUID.eq(uuid.toString()))
                    .fetch();
            for (Record record : result) {
                Item item = MarketManager.getInstance().getItem(record.get("identifier", String.class));
                if (item != null) {
                    content.put(item, record.get("amount", Integer.class));
                }
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getLogger().log(Level.WARNING, e.getMessage(), e);
        }
        return content;
    }

    public static int retrieveCapacity(DSLContext dsl, UUID uuid) {
        int capacity = Config.getInstance().getDefaultSlots();
        try {
            capacity = dsl.transactionResult(configuration ->
                    dsl.select(CAPACITIES.CAPACITY)
                            .from(CAPACITIES)
                            .where(CAPACITIES.UUID.eq(uuid.toString()))
                            .fetchOptional(CAPACITIES.CAPACITY)
                            .orElseGet(() -> {
                                int def = Config.getInstance().getDefaultSlots();
                                dsl.insertInto(CAPACITIES)
                                        .set(CAPACITIES.UUID, uuid.toString())
                                        .set(CAPACITIES.CAPACITY, def)
                                        .execute();
                                return def;
                            })
            );
        } catch (DataAccessException e) {
            Nascraft.getInstance().getLogger().log(Level.WARNING, e.getMessage(), e);
        }
        return capacity;
    }

}
