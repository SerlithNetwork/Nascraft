package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.commands.resources.NormalisedDate;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.stats.Instant;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static me.biquaternions.nascraft.schema.public_.Tables.*;

public class HistorialData {

    public static void saveDayPrice(DSLContext dsl, Item item, Instant instant) {
        try {
            int today = NormalisedDate.getDays();
            dsl.insertInto(PRICES_DAY)
                    .set(PRICES_DAY.DAY, today)
                    .set(PRICES_DAY.IDENTIFIER, item.getIdentifier())
                    .set(PRICES_DAY.DATE, instant.getLocalDateTime().toString())
                    .set(PRICES_DAY.PRICE, instant.getPrice())
                    .set(PRICES_DAY.VOLUME, instant.getVolume())
                    .execute();
            dsl.deleteFrom(PRICES_DAY)
                    .where(PRICES_DAY.DAY.lessThan(today - 2))
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }

    public static void saveMonthPrice(DSLContext dsl, Item item, Instant instant) {
        try {
            dsl.transaction(configuration -> {
                DSLContext ctx = DSL.using(configuration);

                var lastMonthRecord = ctx.select(PRICES_MONTH.DATE)
                        .from(PRICES_MONTH)
                        .where(PRICES_MONTH.IDENTIFIER.eq(item.getIdentifier()))
                        .orderBy(PRICES_MONTH.ID.desc())
                        .limit(1)
                        .fetchOne();

                int today = NormalisedDate.getDays();
                if (lastMonthRecord == null) {
                    ctx.insertInto(PRICES_MONTH)
                            .set(PRICES_MONTH.DAY, today)
                            .set(PRICES_MONTH.DATE, instant.getLocalDateTime().toString())
                            .set(PRICES_MONTH.IDENTIFIER, item.getIdentifier())
                            .set(PRICES_MONTH.PRICE, instant.getPrice())
                            .set(PRICES_MONTH.VOLUME, instant.getVolume())
                            .execute();
                    return;
                }


                LocalDateTime now = LocalDateTime.now();
                LocalDateTime last = LocalDateTime.parse(lastMonthRecord.getValue(PRICES_MONTH.DATE));
                if (last.isBefore(now.minusHours(4))) {
                    var result = ctx.select(PRICES_MONTH.DATE, PRICES_MONTH.PRICE, PRICES_MONTH.VOLUME)
                            .from(PRICES_MONTH)
                            .where(PRICES_MONTH.IDENTIFIER.eq(item.getIdentifier()))
                            .orderBy(PRICES_MONTH.ID.desc())
                            .limit(48)
                            .fetch();

                    if (result.isEmpty()) {
                        ctx.insertInto(PRICES_MONTH)
                                .set(PRICES_MONTH.DAY, today)
                                .set(PRICES_MONTH.DATE, instant.getLocalDateTime().toString())
                                .set(PRICES_MONTH.IDENTIFIER, item.getIdentifier())
                                .set(PRICES_MONTH.PRICE, instant.getPrice())
                                .set(PRICES_MONTH.VOLUME, instant.getVolume())
                                .execute();
                    } else {
                        double totalPrice = 0.0;
                        int totalVolume = 0;
                        int count = 0;
                        for (var record : result) {
                            LocalDateTime date = LocalDateTime.parse(record.getValue(PRICES_MONTH.DATE));
                            if (date.isAfter(now.minusHours(4))) {
                                totalPrice += record.getValue(PRICES_MONTH.PRICE);
                                totalVolume += record.getValue(PRICES_MONTH.VOLUME);
                                count++;
                            }
                        }

                        if (count > 0) {
                            ctx.insertInto(PRICES_MONTH)
                                    .set(PRICES_MONTH.DAY, today)
                                    .set(PRICES_MONTH.DATE, now.minusHours(2).toString())
                                    .set(PRICES_MONTH.IDENTIFIER, item.getIdentifier())
                                    .set(PRICES_MONTH.PRICE, totalPrice / count)
                                    .set(PRICES_MONTH.VOLUME, totalVolume)
                                    .execute();
                        }
                    }

                    ctx.deleteFrom(PRICES_MONTH)
                            .where(PRICES_MONTH.DAY.lessThan(today - 31))
                            .execute();
                }
            });
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }

    public static void saveHistoryPrices(DSLContext dsl, Item item, Instant instant) {
        try {
            dsl.transaction(configuration -> {
                DSLContext ctx = DSL.using(configuration);

                int today = NormalisedDate.getDays();
                var existing = ctx.select(PRICES_HISTORY.DATE)
                        .from(PRICES_HISTORY)
                        .where(PRICES_HISTORY.DAY.eq(today))
                        .and(PRICES_HISTORY.IDENTIFIER.eq(item.getIdentifier()))
                        .fetchOne();

                if (existing != null) {
                    return;
                }

                var recentMonthly = ctx.select(PRICES_MONTH.DATE, PRICES_MONTH.PRICE, PRICES_MONTH.VOLUME)
                        .from(PRICES_MONTH)
                        .where(PRICES_MONTH.IDENTIFIER.eq(item.getIdentifier()))
                        .orderBy(PRICES_MONTH.ID.desc())
                        .limit(6)
                        .fetch();

                if (recentMonthly.isEmpty()) {
                    ctx.insertInto(PRICES_HISTORY)
                            .set(PRICES_HISTORY.DAY, today)
                            .set(PRICES_HISTORY.DATE, instant.getLocalDateTime().toString())
                            .set(PRICES_HISTORY.IDENTIFIER, item.getIdentifier())
                            .set(PRICES_HISTORY.PRICE, instant.getPrice())
                            .set(PRICES_HISTORY.VOLUME, instant.getVolume())
                            .execute();
                    return;
                }

                double totalPrice = 0.0;
                int totalVolume = 0;
                int count = 0;
                LocalDateTime now = LocalDateTime.now();
                for (var record : recentMonthly) {
                    LocalDateTime date = LocalDateTime.parse(record.getValue(PRICES_MONTH.DATE));
                    if (date.isAfter(now.minusHours(24))) {
                        totalPrice += record.getValue(PRICES_MONTH.PRICE);
                        totalVolume += record.getValue(PRICES_MONTH.VOLUME);
                        count++;
                    }
                }

                if (count <= 0) {
                    return;
                }

                ctx.insertInto(PRICES_HISTORY)
                        .set(PRICES_HISTORY.DAY, today)
                        .set(PRICES_HISTORY.DATE, now.minusHours(12).toString())
                        .set(PRICES_HISTORY.IDENTIFIER, item.getIdentifier())
                        .set(PRICES_HISTORY.PRICE, totalPrice / count)
                        .set(PRICES_HISTORY.VOLUME, totalVolume)
                        .execute();
            });
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }


    public static List<Instant> getDayPrices(DSLContext dsl, Item item) {
        try {
            return dsl.transactionResult(configuration -> {
                DSLContext ctx = DSL.using(configuration);
                List<Instant> prices = new LinkedList<>();

                var record = ctx.select(PRICES_DAY.DATE)
                        .from(PRICES_DAY)
                        .where(PRICES_DAY.IDENTIFIER.eq(item.getIdentifier()))
                        .orderBy(PRICES_DAY.ID.desc())
                        .limit(1)
                        .fetchOne();

                if (record == null) {
                    prices.add(new Instant(LocalDateTime.now().minusHours(24), 0, 0));
                    prices.add(new Instant(LocalDateTime.now().minusMinutes(5), 0, 0));
                    prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));
                    return prices;
                }

                var result = ctx.select(PRICES_DAY.DATE, PRICES_DAY.PRICE, PRICES_DAY.VOLUME)
                        .from(PRICES_DAY)
                        .where(PRICES_DAY.IDENTIFIER.eq(item.getIdentifier()))
                        .orderBy(PRICES_DAY.ID.desc())
                        .limit(288)
                        .fetch();

                for (var r : result) {
                    LocalDateTime time = LocalDateTime.parse(r.getValue(PRICES_DAY.DATE));
                    double price = r.getValue(PRICES_DAY.PRICE);
                    if (time.isAfter(LocalDateTime.now().minusHours(24)) && Math.round(price) != 0) {
                        prices.add(new Instant(
                                time,
                                r.getValue(PRICES_DAY.PRICE),
                                r.getValue(PRICES_DAY.VOLUME)
                        ));
                    }
                }
                prices.addFirst(new Instant(LocalDateTime.now().minusHours(24), 0, 0));
                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));
                return prices;
            });
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public static List<Instant> getMonthPrices(DSLContext dsl, Item item) {
        try {
            return dsl.transactionResult(configuration -> {
                DSLContext ctx = DSL.using(configuration);
                List<Instant> prices = new LinkedList<>();

                var record = ctx.select(PRICES_MONTH.DATE)
                        .from(PRICES_MONTH)
                        .where(PRICES_MONTH.IDENTIFIER.eq(item.getIdentifier()))
                        .orderBy(PRICES_MONTH.ID.desc())
                        .limit(1)
                        .fetchOne();

                // noinspection DuplicatedCode
                if (record == null) {
                    prices.add(new Instant(LocalDateTime.now().minusDays(30), 0, 0));
                    prices.add(new Instant(LocalDateTime.now().minusMinutes(5), 0, 0));
                    prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));
                    return prices;
                }

                var result = ctx.select(PRICES_MONTH.DATE, PRICES_MONTH.PRICE, PRICES_MONTH.VOLUME)
                        .from(PRICES_MONTH)
                        .where(PRICES_MONTH.IDENTIFIER.eq(item.getIdentifier()))
                        .orderBy(PRICES_MONTH.ID.desc())
                        .limit(400)
                        .fetch();

                for (var r : result) {
                    LocalDateTime time = LocalDateTime.parse(r.getValue(PRICES_MONTH.DATE));
                    if (time.isAfter(LocalDateTime.now().minusDays(30))) {
                        prices.add(new Instant(
                                time,
                                r.getValue(PRICES_MONTH.PRICE),
                                r.getValue(PRICES_MONTH.VOLUME)
                        ));
                    }
                }
                prices.addFirst(new Instant(LocalDateTime.now().minusDays(30), 0, 0));
                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));
                return prices;
            });
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public static List<Instant> getYearPrices(DSLContext dsl, Item item) {
        try {
            return dsl.transactionResult(configuration ->  {
                DSLContext ctx = DSL.using(configuration);
                List<Instant> prices = new LinkedList<>();

                var record = ctx.select(PRICES_HISTORY.DAY)
                        .from(PRICES_HISTORY)
                        .where(PRICES_HISTORY.IDENTIFIER.eq(item.getIdentifier()))
                        .orderBy(PRICES_HISTORY.DAY.desc())
                        .limit(1)
                        .fetchOne();

                if (record == null) {
                    prices.add(new Instant(LocalDateTime.now().minusDays(365), 0, 0));
                    prices.add(new Instant(LocalDateTime.now().minusMinutes(5), 0, 0));
                    prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));
                    return prices;
                }

                var result = ctx.select(PRICES_HISTORY.DAY, PRICES_HISTORY.PRICE, PRICES_HISTORY.VOLUME)
                        .from(PRICES_HISTORY)
                        .where(PRICES_HISTORY.IDENTIFIER.eq(item.getIdentifier()))
                        .orderBy(PRICES_HISTORY.DAY.desc())
                        .limit(385)
                        .fetch();

                for (var r : result) {
                    LocalDateTime time = LocalDateTime.of(2023, 1, 1, 1, 1).plusDays(r.getValue(PRICES_HISTORY.DAY));
                    if (time.isAfter(LocalDateTime.now().minusDays(365))) {
                        prices.add(new Instant(
                                time,
                                r.getValue(PRICES_HISTORY.PRICE),
                                r.getValue(PRICES_HISTORY.VOLUME)
                        ));
                    }
                }
                prices.add(new Instant(LocalDateTime.now().minusDays(365), 0, 0));
                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));
                return prices;
            });
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public static List<Instant> getAllPrices(DSLContext dsl, Item item) {
        try {
            return dsl.transactionResult(configuration -> {
                DSLContext ctx = DSL.using(configuration);
                List<Instant> prices = new LinkedList<>();

                var record = ctx.select(PRICES_HISTORY.DAY)
                        .from(PRICES_HISTORY)
                        .where(PRICES_HISTORY.IDENTIFIER.eq(item.getIdentifier()))
                        .orderBy(PRICES_HISTORY.DAY.desc())
                        .limit(1)
                        .fetchOne();

                // noinspection DuplicatedCode
                if (record == null) {
                    prices.add(new Instant(LocalDateTime.now().minusDays(30), 0, 0));
                    prices.add(new Instant(LocalDateTime.now().minusMinutes(5), 0, 0));
                    prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));
                    return prices;
                }

                var result = ctx.select(PRICES_HISTORY.DAY, PRICES_HISTORY.PRICE, PRICES_HISTORY.VOLUME)
                        .from(PRICES_HISTORY)
                        .where(PRICES_HISTORY.IDENTIFIER.eq(item.getIdentifier()))
                        .orderBy(PRICES_HISTORY.DAY.desc())
                        .fetch();

                LocalDateTime time = LocalDateTime.of(2023, 1, 1, 1, 1);
                for (var r : result) {
                    prices.add(new Instant(
                            time.plusDays(r.getValue(PRICES_HISTORY.DAY)),
                            r.getValue(PRICES_HISTORY.PRICE),
                            r.getValue(PRICES_HISTORY.VOLUME)
                    ));
                }
                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));
                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));
                return prices;
            });
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public static Double getPriceOfDay(DSLContext dsl, String identifier, int day) {
        try {
            var record = dsl.select(PRICES_HISTORY.PRICE)
                    .from(PRICES_HISTORY)
                    .where(PRICES_HISTORY.IDENTIFIER.eq(identifier))
                    .and(PRICES_HISTORY.DAY.eq(day))
                    .fetchOne();

            if (record != null) {
                return record.getValue(PRICES_HISTORY.PRICE);
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return 0.0;
    }

}
