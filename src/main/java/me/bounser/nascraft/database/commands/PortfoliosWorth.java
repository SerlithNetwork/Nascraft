package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.commands.resources.NormalisedDate;
import me.bounser.nascraft.portfolio.Portfolio;
import me.bounser.nascraft.portfolio.PortfoliosManager;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;

import static me.biquaternions.nascraft.schema.public_.Tables.PORTFOLIOS_WORTH;

public class PortfoliosWorth {

    public static void saveOrUpdateWorth(DSLContext dsl, UUID uuid, int day, double worth) {
        try {
            dsl.transaction(configuration -> {
                DSLContext ctx = DSL.using(configuration);
                var record = ctx.select(PORTFOLIOS_WORTH.ID)
                        .from(PORTFOLIOS_WORTH)
                        .where(PORTFOLIOS_WORTH.UUID.eq(uuid.toString()))
                        .and(PORTFOLIOS_WORTH.DAY.eq(day))
                        .fetchOne();

                if (record != null) {
                    ctx.update(PORTFOLIOS_WORTH)
                            .set(PORTFOLIOS_WORTH.WORTH, worth)
                            .where(PORTFOLIOS_WORTH.UUID.eq(uuid.toString()))
                            .and(PORTFOLIOS_WORTH.DAY.eq(day))
                            .execute();
                } else {
                    ctx.insertInto(PORTFOLIOS_WORTH)
                            .set(PORTFOLIOS_WORTH.UUID, uuid.toString())
                            .set(PORTFOLIOS_WORTH.DAY, day)
                            .set(PORTFOLIOS_WORTH.WORTH, worth)
                            .execute();
                }
            });
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().error(e.getMessage(), e);
        }
    }

    public static void saveOrUpdateWorthToday(DSLContext dsl, UUID uuid, double worth) {
        int today = NormalisedDate.getDays();
        PortfoliosWorth.saveOrUpdateWorth(dsl, uuid, today, worth);
    }

    public static HashMap<UUID, Portfolio> getTopWorth(DSLContext dsl, int n) {
        LinkedHashMap<UUID, Portfolio> result = new LinkedHashMap<>();
        try {
            var rs = dsl.select(PORTFOLIOS_WORTH.UUID, PORTFOLIOS_WORTH.WORTH)
                    .from(PORTFOLIOS_WORTH)
                    .where(DSL.row(PORTFOLIOS_WORTH.UUID, PORTFOLIOS_WORTH.DAY)
                            .in(DSL.select(PORTFOLIOS_WORTH.UUID, DSL.max(PORTFOLIOS_WORTH.DAY))
                                    .from(PORTFOLIOS_WORTH)
                                    .groupBy(PORTFOLIOS_WORTH.UUID)
                            ))
                    .orderBy(PORTFOLIOS_WORTH.WORTH.desc())
                    .limit(n)
                    .fetch();

            for (var record : rs) {
                UUID uuid = UUID.fromString(record.getValue(PORTFOLIOS_WORTH.UUID));
                result.put(uuid, PortfoliosManager.getInstance().getPortfolio(uuid));
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().error(e.getMessage(), e);
        }
        return result;
    }

    public static double getLatestWorth(DSLContext dsl, UUID uuid) {
        try {
            var record = dsl.select(PORTFOLIOS_WORTH.WORTH)
                    .from(PORTFOLIOS_WORTH)
                    .where(PORTFOLIOS_WORTH.UUID.eq(uuid.toString()))
                    .orderBy(PORTFOLIOS_WORTH.DAY.desc())
                    .limit(1)
                    .fetchOne();

            if (record != null) {
                return record.get(PORTFOLIOS_WORTH.WORTH);
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().error(e.getMessage(), e);
        }
        return 0;
    }
}
