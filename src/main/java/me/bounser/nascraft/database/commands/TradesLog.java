package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.database.commands.resources.NormalisedDate;
import me.bounser.nascraft.database.commands.resources.Trade;
import me.bounser.nascraft.formatter.RoundUtils;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static me.biquaternions.nascraft.schema.public_.Tables.TRADE_LOG;

public class TradesLog {

    public static void saveTrade(DSLContext dsl, Trade trade) {
        try {
            dsl.insertInto(TRADE_LOG)
                    .set(TRADE_LOG.UUID, trade.getUuid().toString())
                    .set(TRADE_LOG.DAY, NormalisedDate.getDays())
                    .set(TRADE_LOG.DATE, NormalisedDate.formatDateTime(LocalDateTime.now()))
                    .set(TRADE_LOG.IDENTIFIER, trade.getItem().getIdentifier())
                    .set(TRADE_LOG.AMOUNT, trade.getAmount())
                    .set(TRADE_LOG.VALUE, (double) RoundUtils.round(trade.getValue()))
                    .set(TRADE_LOG.BUY, trade.isBuy())
                    .set(TRADE_LOG.DISCORD, trade.throughDiscord())
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    public static List<Trade> retrieveTrades(DSLContext dsl, UUID uuid, int offset, int limit) {
        if (uuid == null) {
            return Collections.emptyList();
        }
        
        try {
            List<Trade> trades = new ArrayList<>();
            var result = dsl.selectFrom(TRADE_LOG)
                    .where(TRADE_LOG.UUID.eq(uuid.toString()))
                    .orderBy(TRADE_LOG.ID.desc())
                    .limit(limit)
                    .offset(offset)
                    .fetch();

            //noinspection DuplicatedCode
            for (Record record : result) {
                trades.add(
                        new Trade(
                                MarketManager.getInstance().getItem(record.get(TRADE_LOG.IDENTIFIER, String.class)),
                                NormalisedDate.parseDateTime(record.get(TRADE_LOG.DATE, String.class)),
                                record.getValue(TRADE_LOG.VALUE),
                                record.getValue(TRADE_LOG.AMOUNT),
                                record.getValue(TRADE_LOG.BUY),
                                record.getValue(TRADE_LOG.DISCORD),
                                uuid
                        )
                );
            }
            return trades;
            
        } catch (DataAccessException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
        return Collections.emptyList();
    }

    public static List<Trade> retrieveTrades(DSLContext dsl, UUID uuid, Item item, int offset, int limit) {
        if (uuid == null) {
            return Collections.emptyList();
        }
        
        try {
            List<Trade> trades = new ArrayList<>();
            var result = dsl.selectFrom(TRADE_LOG)
                    .where(TRADE_LOG.UUID.eq(uuid.toString()))
                    .and(TRADE_LOG.IDENTIFIER.eq(item.getIdentifier()))
                    .orderBy(TRADE_LOG.ID.desc())
                    .limit(limit)
                    .offset(offset)
                    .fetch();

            //noinspection DuplicatedCode
            for (Record record : result) {
                trades.add(new Trade(
                        MarketManager.getInstance().getItem(record.getValue(TRADE_LOG.IDENTIFIER)),
                        NormalisedDate.parseDateTime(record.getValue(TRADE_LOG.DATE)),
                        record.get(TRADE_LOG.VALUE),
                        record.get(TRADE_LOG.AMOUNT),
                        record.get(TRADE_LOG.BUY),
                        record.get(TRADE_LOG.DISCORD),
                        uuid
                ));
            }
            return trades;
        } catch (DataAccessException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
        return Collections.emptyList();
    }

    public static List<Trade> retrieveTrades(DSLContext dsl, Item item, int offset, int limit) {
        try {
            List<Trade> trades = new ArrayList<>();
            var result = dsl.selectFrom(TRADE_LOG)
                    .where(TRADE_LOG.IDENTIFIER.eq(item.getIdentifier()))
                    .orderBy(TRADE_LOG.ID.desc())
                    .limit(limit)
                    .offset(offset)
                    .fetch();
            
            for (Record record : result) {
                trades.add(new Trade(
                        item,
                        NormalisedDate.parseDateTime(record.getValue(TRADE_LOG.DATE)),
                        record.getValue(TRADE_LOG.VALUE),
                        record.getValue(TRADE_LOG.AMOUNT),
                        record.getValue(TRADE_LOG.BUY),
                        record.getValue(TRADE_LOG.DISCORD),
                        UUID.fromString(record.getValue(TRADE_LOG.UUID))
                ));
            }
            return trades;
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public static List<Trade> retrieveLastTrades(DSLContext dsl, int offset, int limit) {
        try {
            List<Trade> trades = new ArrayList<>();
            var result = dsl.selectFrom(TRADE_LOG)
                    .orderBy(TRADE_LOG.ID.desc())
                    .limit(limit)
                    .offset(offset)
                    .fetch();
            
            for (Record record : result) {
                trades.add(new Trade(
                        MarketManager.getInstance().getItem(record.getValue(TRADE_LOG.IDENTIFIER)),
                        NormalisedDate.parseDateTime(record.getValue(TRADE_LOG.DATE)),
                        record.getValue(TRADE_LOG.VALUE),
                        record.getValue(TRADE_LOG.AMOUNT),
                        record.getValue(TRADE_LOG.BUY),
                        record.getValue(TRADE_LOG.DISCORD),
                        UUID.fromString(record.getValue(TRADE_LOG.UUID))
                ));
            }
            return trades;
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public static void purgeHistory(DSLContext dsl) {
        int offset = Config.getInstance().getDatabasePurgeDays();
        if (offset == -1) {
            return;
        }

        try {
            dsl.deleteFrom(TRADE_LOG)
                    .where(TRADE_LOG.DAY.lessThan(NormalisedDate.getDays() - offset))
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

}
