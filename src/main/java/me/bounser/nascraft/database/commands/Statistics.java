package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.chart.cpi.CPIInstant;
import me.bounser.nascraft.database.commands.resources.DayInfo;
import me.bounser.nascraft.database.commands.resources.NormalisedDate;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.stats.Instant;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static me.biquaternions.nascraft.schema.public_.Tables.CPI;
import static me.biquaternions.nascraft.schema.public_.Tables.FLOWS;

public class Statistics {

    public static void saveCPI(DSLContext dsl, float value) {
        try {
            dsl.insertInto(CPI)
                    .set(CPI.DAY, NormalisedDate.getDays())
                    .set(CPI.DATE, LocalDate.now().toString())
                    .set(CPI.VALUE, (double) value)
                    .onDuplicateKeyIgnore()
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }

    public static List<CPIInstant> getAllCPI(DSLContext dsl) {
        List<CPIInstant> cpiInstants = new ArrayList<>();
        try {
            var result = dsl.selectFrom(CPI)
                    .fetch();

            for (var record : result) {
                cpiInstants.add(new CPIInstant(record.getValue().floatValue(), LocalDateTime.parse(record.getDate())));
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return cpiInstants;
    }

    public static List<Instant> getPriceAgainstCPI(DSLContext dsl, Item item) {
        try {
            return dsl.transactionResult(configuration -> {
                DSLContext ctx = DSL.using(configuration);
                var record = ctx.select(DSL.min(CPI.DAY))
                        .from(CPI)
                        .fetchOne();

                int minValue = -1;
                if (record != null) {
                    minValue = record.value1();
                }

                if (minValue == -1) {
                    return Collections.singletonList(new Instant(LocalDateTime.now(), item.getPrice().getValue(), 0));
                }

                if (NormalisedDate.getDays() - 30 < minValue) {
                    return HistorialData.getMonthPrices(ctx, item);
                }
                return HistorialData.getAllPrices(ctx, item);
            });
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public static void addTransaction(DSLContext dsl, double newFlow, double effectiveTaxes) {
        try {
            dsl.insertInto(FLOWS)
                    .set(FLOWS.DAY, NormalisedDate.getDays())
                    .set(FLOWS.FLOW, newFlow)
                    .set(FLOWS.TAXES, effectiveTaxes)
                    .set(FLOWS.OPERATIONS, 1)
                    .onDuplicateKeyUpdate()
                    .set(FLOWS.FLOW, FLOWS.FLOW.plus(newFlow))
                    .set(FLOWS.TAXES, FLOWS.TAXES.plus(effectiveTaxes))
                    .set(FLOWS.OPERATIONS, FLOWS.OPERATIONS.plus(1))
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }

    public static List<DayInfo> getDayInfos(DSLContext dsl) {
        List<DayInfo> dayInfos = new ArrayList<>();
        try {
            var result = dsl.selectFrom(FLOWS)
                    .fetch();

            for (var record : result) {
                dayInfos.add(new DayInfo(
                        record.getDay(),
                        record.getFlow(),
                        record.getTaxes()
                ));
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return dayInfos;
    }

    public static double getAllTaxesCollected(DSLContext dsl) {
        try {
            var record = dsl.select(FLOWS.TAXES)
                    .from(FLOWS)
                    .orderBy(FLOWS.DAY.desc())
                    .fetchOne();

            if (record != null) {
                return record.getValue(FLOWS.TAXES);
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return 0.0;
    }

}
