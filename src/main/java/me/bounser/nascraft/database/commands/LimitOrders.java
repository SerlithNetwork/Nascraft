package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.limitorders.LimitOrder;
import me.bounser.nascraft.market.limitorders.LimitOrdersManager;
import me.bounser.nascraft.market.limitorders.OrderType;
import me.bounser.nascraft.market.unit.Item;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;

import java.time.LocalDateTime;
import java.util.UUID;

import static me.biquaternions.nascraft.schema.public_.Tables.LIMIT_ORDERS;

public class LimitOrders {

    public static void addLimitOrder(DSLContext dsl, UUID uuid, LocalDateTime expiration, Item item, int type, double price, int amount) {
        try {
            dsl.insertInto(LIMIT_ORDERS)
                    .set(LIMIT_ORDERS.EXPIRATION, expiration.toString())
                    .set(LIMIT_ORDERS.UUID, uuid.toString())
                    .set(LIMIT_ORDERS.IDENTIFIER, item.getIdentifier())
                    .set(LIMIT_ORDERS.TYPE, type)
                    .set(LIMIT_ORDERS.PRICE, price)
                    .set(LIMIT_ORDERS.TO_COMPLETE, amount)
                    .set(LIMIT_ORDERS.COMPLETED, 0)
                    .set(LIMIT_ORDERS.COST, 0)
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }

    public static void updateLimitOrder(DSLContext dsl, UUID uuid, Item item, int completed, double cost) {
        try {
            dsl.update(LIMIT_ORDERS)
                    .set(LIMIT_ORDERS.COMPLETED, completed)
                    .set(LIMIT_ORDERS.COST, (int) cost)
                    .where(LIMIT_ORDERS.UUID.eq(uuid.toString()))
                    .and(LIMIT_ORDERS.IDENTIFIER.eq(item.getIdentifier()))
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }

    public static void removeLimitOrder(DSLContext dsl, String uuid, String identifier) {
        try {
            dsl.deleteFrom(LIMIT_ORDERS)
                    .where(LIMIT_ORDERS.UUID.eq(uuid))
                    .and(LIMIT_ORDERS.IDENTIFIER.eq(identifier))
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }

    public static void retrieveLimitOrders(DSLContext dsl) {
        try {
            var result = dsl.selectFrom(LIMIT_ORDERS)
                    .fetch();

            for (var record : result) {
                LimitOrdersManager.getInstance().registerLimitOrder(
                        new LimitOrder(
                                UUID.fromString(record.getUuid()),
                                MarketManager.getInstance().getItem(record.getIdentifier()),
                                LocalDateTime.parse(record.getExpiration()),
                                record.getToComplete(),
                                record.getCompleted(),
                                record.getPrice(),
                                record.getCost(),
                                (record.getType() == 1 ? OrderType.LIMIT_BUY : OrderType.LIMIT_SELL)
                        )
                );
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }

}
