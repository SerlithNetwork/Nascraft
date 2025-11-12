package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.commands.resources.NormalisedDate;
import me.bounser.nascraft.market.unit.Item;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static me.biquaternions.nascraft.schema.public_.Tables.PORTFOLIOS_LOG;

public class PortfoliosLog {

    public static void logContribution(DSLContext dsl, UUID uuid, Item item, int amount) {
        double contribution = item.getPrice().getValue() * amount;
        try {
            dsl.insertInto(PORTFOLIOS_LOG)
                    .set(PORTFOLIOS_LOG.UUID, uuid.toString())
                    .set(PORTFOLIOS_LOG.IDENTIFIER, item.getIdentifier())
                    .set(PORTFOLIOS_LOG.DAY, NormalisedDate.getDays())
                    .set(PORTFOLIOS_LOG.AMOUNT, amount)
                    .set(PORTFOLIOS_LOG.CONTRIBUTION, contribution)
                    .onDuplicateKeyUpdate()
                    .set(PORTFOLIOS_LOG.AMOUNT, PORTFOLIOS_LOG.AMOUNT.plus(amount))
                    .set(PORTFOLIOS_LOG.CONTRIBUTION, PORTFOLIOS_LOG.CONTRIBUTION.plus(contribution))
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }

    public static void logWithdraw(DSLContext dsl, UUID uuid, Item item, int amount) {
        try {
            dsl.transaction(context -> {
                DSLContext ctx = DSL.using(context);

                var latest = ctx.select(PORTFOLIOS_LOG.CONTRIBUTION, PORTFOLIOS_LOG.AMOUNT, PORTFOLIOS_LOG.DAY)
                        .from(PORTFOLIOS_LOG)
                        .where(PORTFOLIOS_LOG.UUID.eq(uuid.toString()))
                        .and(PORTFOLIOS_LOG.IDENTIFIER.eq(item.getIdentifier()))
                        .orderBy(PORTFOLIOS_LOG.DAY.desc())
                        .limit(1)
                        .fetchOne();

                if (latest == null) {
                    return;
                }

                int today = NormalisedDate.getDays();
                int latestAmount = latest.getValue(PORTFOLIOS_LOG.AMOUNT);
                double latestContribution = latest.getValue(PORTFOLIOS_LOG.CONTRIBUTION);
                int latestDay = latest.getValue(PORTFOLIOS_LOG.DAY);

                if (latestAmount <= amount) {
                    ctx.deleteFrom(PORTFOLIOS_LOG)
                            .where(PORTFOLIOS_LOG.UUID.eq(uuid.toString()))
                            .and(PORTFOLIOS_LOG.IDENTIFIER.eq(item.getIdentifier()))
                            .and(PORTFOLIOS_LOG.DAY.eq(today))
                            .execute();
                } else {
                    int newAmount = latestAmount - amount;
                    double newContribution = latestContribution / latestAmount;

                    if (latestDay == today) {
                        ctx.update(PORTFOLIOS_LOG)
                                .set(PORTFOLIOS_LOG.AMOUNT, newAmount)
                                .set(PORTFOLIOS_LOG.CONTRIBUTION, newContribution)
                                .where(PORTFOLIOS_LOG.UUID.eq(uuid.toString()))
                                .and(PORTFOLIOS_LOG.IDENTIFIER.eq(item.getIdentifier()))
                                .execute();
                    } else {
                        ctx.insertInto(PORTFOLIOS_LOG)
                                .set(PORTFOLIOS_LOG.UUID, uuid.toString())
                                .set(PORTFOLIOS_LOG.IDENTIFIER, item.getIdentifier())
                                .set(PORTFOLIOS_LOG.DAY, today)
                                .set(PORTFOLIOS_LOG.AMOUNT, newAmount)
                                .set(PORTFOLIOS_LOG.CONTRIBUTION, newContribution)
                                .execute();
                    }
                }

            });
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }

    public static HashMap<Integer, Double> getContributionChangeEachDay(DSLContext dsl, UUID uuid) {
        HashMap<Integer, Double> dayAndContribution = new HashMap<>();
        try {
            var result = dsl.select(PORTFOLIOS_LOG.CONTRIBUTION, PORTFOLIOS_LOG.AMOUNT)
                    .from(PORTFOLIOS_LOG)
                    .where(PORTFOLIOS_LOG.UUID.eq(uuid.toString()))
                    .orderBy(PORTFOLIOS_LOG.DAY.desc())
                    .fetch();

            for (Record record : result) {
                int day = record.getValue(PORTFOLIOS_LOG.DAY);
                dayAndContribution.merge(day, record.getValue(PORTFOLIOS_LOG.CONTRIBUTION), Double::sum);
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return dayAndContribution;
    }

    public static HashMap<Integer, HashMap<String, Integer>> getCompositionEachDay(DSLContext dsl, UUID uuid) {
        HashMap<Integer, HashMap<String, Integer>> dayAndComposition = new HashMap<>();
        try {
            var result = dsl.select(PORTFOLIOS_LOG.IDENTIFIER, PORTFOLIOS_LOG.AMOUNT)
                    .from(PORTFOLIOS_LOG)
                    .where(PORTFOLIOS_LOG.UUID.eq(uuid.toString()))
                    .orderBy(PORTFOLIOS_LOG.DAY.desc())
                    .fetch();

            for (Record record : result) {
                String identifier = record.getValue(PORTFOLIOS_LOG.IDENTIFIER);
                int day = record.getValue(PORTFOLIOS_LOG.DAY);
                int amount = record.getValue(PORTFOLIOS_LOG.AMOUNT);
                dayAndComposition.merge(day, new HashMap<>(Map.of(identifier, amount)), (current, introduced) -> {
                    current.putAll(introduced);
                    return current;
                });
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return dayAndComposition;
    }

    public static int getFirstDay(DSLContext dsl, UUID uuid) {
        try {
            var day = dsl.select(DSL.min(PORTFOLIOS_LOG.DAY).as("first_day"))
                    .from(PORTFOLIOS_LOG)
                    .where(PORTFOLIOS_LOG.UUID.eq(uuid.toString()))
                    .fetchOne(r -> r != null ? r.get("first_day", Integer.class) : null);

            return day != null ? day : NormalisedDate.getDays();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return NormalisedDate.getDays();
    }

}
