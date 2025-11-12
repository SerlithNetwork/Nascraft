package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.Nascraft;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.util.HashMap;
import java.util.UUID;

import static me.biquaternions.nascraft.schema.public_.Tables.INTERESTS;
import static me.biquaternions.nascraft.schema.public_.Tables.LOANS;

public class Debt {

    public static void increaseDebt(DSLContext dsl, UUID uuid, double debt) {
        try {
            dsl.insertInto(LOANS)
                    .set(LOANS.UUID, uuid.toString())
                    .set(LOANS.DEBT, debt)
                    .onDuplicateKeyUpdate()
                    .set(LOANS.DEBT, LOANS.DEBT.plus(debt))
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }

    public static void decreaseDebt(DSLContext dsl, UUID uuid, double debt) {
        try {
            dsl.transaction(configuration -> {
                DSLContext ctx = DSL.using(configuration);

                var record = ctx.select(LOANS.DEBT)
                        .from(LOANS)
                        .where(LOANS.UUID.eq(uuid.toString()))
                        .fetchOne();

                if (record == null) {
                    return;
                }

                double currentDebt = record.getValue(LOANS.DEBT);
                double newDebt = currentDebt - debt;
                if (newDebt < 0) {
                    ctx.deleteFrom(LOANS)
                            .where(LOANS.UUID.eq(uuid.toString()))
                            .execute();
                } else {
                    ctx.update(LOANS)
                            .set(LOANS.DEBT, newDebt)
                            .where(LOANS.UUID.eq(uuid.toString()))
                            .execute();
                }
            });
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }

    public static double getDebt(DSLContext dsl, UUID uuid) {
        try {
            var record = dsl.select(LOANS.DEBT)
                    .from(LOANS)
                    .where(LOANS.UUID.eq(uuid.toString()))
                    .fetchOne();

            if (record != null) {
                return record.getValue(LOANS.DEBT);
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return 0;
    }

    public static HashMap<UUID, Double> getUUIDAndDebt(DSLContext dsl) {
        HashMap<UUID, Double> debtors = new HashMap<>();
        try {
            var result = dsl.select(LOANS.UUID, LOANS.DEBT)
                    .from(LOANS)
                    .where(LOANS.DEBT.greaterThan(0.0))
                    .fetch();

            for (var record : result) {
                debtors.put(UUID.fromString(record.getValue(LOANS.UUID)), record.getValue(LOANS.DEBT));
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return debtors;
    }

    public static void addInterestPaid(DSLContext dsl, UUID uuid, Double interest) {
        try {
            dsl.transaction(configuration -> {
                DSLContext ctx = DSL.using(configuration);

                var record = ctx.select(INTERESTS.PAID)
                        .from(INTERESTS)
                        .where(INTERESTS.UUID.eq(uuid.toString()))
                        .fetchOne();

                if (record != null) {
                    ctx.update(INTERESTS)
                            .set(INTERESTS.PAID, record.getValue(INTERESTS.PAID) + interest)
                            .where(INTERESTS.UUID.eq(uuid.toString()))
                            .execute();
                } else {
                    ctx.insertInto(INTERESTS)
                            .set(INTERESTS.UUID, uuid.toString())
                            .set(INTERESTS.PAID, interest)
                            .execute();
                }
            });
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }

    public static HashMap<UUID, Double> getUUIDAndInterestsPaid(DSLContext dsl) {
        HashMap<UUID, Double> payers = new HashMap<>();
        try {
            var result = dsl.select(INTERESTS.UUID, INTERESTS.PAID)
                    .from(INTERESTS)
                    .fetch();
            for (var record : result) {
                payers.put(UUID.fromString(record.getValue(INTERESTS.UUID)), record.getValue(INTERESTS.PAID));
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return payers;
    }

    public static double getInterestsPaid(DSLContext dsl, UUID uuid) {
        try {
            var record = dsl.select(INTERESTS.PAID)
                    .from(INTERESTS)
                    .where(INTERESTS.UUID.eq(uuid.toString()))
                    .fetchOne();

            if (record != null) {
                return record.getValue(INTERESTS.PAID);
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return 0;
    }

    public static double getAllOutstandingDebt(DSLContext dsl) {
        try {
            var record = dsl.select(DSL.sum(LOANS.DEBT))
                    .from(LOANS)
                    .fetchOne();

            if (record != null) {
                return record.component1().doubleValue();
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return 0;
    }

    public static double getAllInterestsPaid(DSLContext dsl) {
        try {
            var record = dsl.select(DSL.sum(INTERESTS.PAID))
                    .from(INTERESTS)
                    .fetchOne();

            if (record != null) {
                return record.component1().doubleValue();
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return 0;
    }

}
