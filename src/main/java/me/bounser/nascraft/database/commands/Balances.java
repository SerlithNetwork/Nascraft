package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.commands.resources.NormalisedDate;
import me.bounser.nascraft.managers.MoneyManager;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static me.biquaternions.nascraft.schema.public_.Tables.BALANCES;
import static me.biquaternions.nascraft.schema.public_.Tables.MONEY_SUPPLY;

public class Balances {

    public static void updateBalance(DSLContext dsl, UUID uuid) {

        Player player = Bukkit.getPlayer(uuid);
        if (player == null || player.isOp()) {
            return;
        }

        try {
            dsl.transaction(configuration -> {
                DSLContext ctx = DSL.using(configuration);

                double currentBalance = MoneyManager.getInstance().getBalance(player, CurrenciesManager.getInstance().getDefaultCurrency());
                double pastBalance = 0.0;

                var pastBalanceRecord = ctx.select(BALANCES.BALANCE)
                        .from(BALANCES)
                        .where(BALANCES.UUID.eq(uuid.toString()))
                        .fetchOne();
                if (pastBalanceRecord != null) {
                    pastBalance = pastBalanceRecord.getValue(BALANCES.BALANCE);
                }

                ctx.insertInto(BALANCES)
                        .set(BALANCES.UUID, uuid.toString())
                        .set(BALANCES.BALANCE, currentBalance)
                        .onDuplicateKeyUpdate()
                        .set(BALANCES.BALANCE, currentBalance)
                        .execute();

                double balanceDifference = currentBalance - pastBalance;
                if (balanceDifference == 0.0) {
                    return;
                }

                int today = NormalisedDate.getDays();
                var supplyRecord = ctx.select(MONEY_SUPPLY.DAY, MONEY_SUPPLY.SUPPLY)
                        .from(MONEY_SUPPLY)
                        .where(MONEY_SUPPLY.DAY.le(today))
                        .orderBy(MONEY_SUPPLY.DAY.desc())
                        .limit(1)
                        .fetchOne();

                if (supplyRecord != null && supplyRecord.get(MONEY_SUPPLY.DAY) == today) {
                    double newSupply = supplyRecord.get(MONEY_SUPPLY.SUPPLY) + balanceDifference;
                    ctx.update(MONEY_SUPPLY)
                            .set(MONEY_SUPPLY.SUPPLY, newSupply)
                            .where(MONEY_SUPPLY.DAY.eq(today))
                            .execute();
                } else {
                    ctx.insertInto(MONEY_SUPPLY)
                            .set(MONEY_SUPPLY.DAY, today)
                            .set(MONEY_SUPPLY.SUPPLY, balanceDifference)
                            .execute();
                }
            });
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn("Database operation failed for player {}", player.getName(), e);
        }
    }

    public static Map<Integer, Double> getMoneySupplyHistory(DSLContext dsl) {
        Map<Integer, Double> supplyHistory = new HashMap<>();
        try {
            var result = dsl.selectFrom(MONEY_SUPPLY)
                    .orderBy(MONEY_SUPPLY.DAY.asc())
                    .fetch();

            for (var record : result) {
                supplyHistory.put(record.getDay(), record.getSupply());
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn("Failed to retrieve money supply history from database.", e);
        }
        return supplyHistory;
    }


}
