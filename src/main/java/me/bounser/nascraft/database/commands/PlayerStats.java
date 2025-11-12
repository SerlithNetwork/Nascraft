package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.commands.resources.NormalisedDate;
import me.bounser.nascraft.managers.DebtManager;
import me.bounser.nascraft.portfolio.PortfoliosManager;
import me.bounser.nascraft.web.dto.PlayerStatsDTO;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static me.biquaternions.nascraft.schema.public_.Tables.PLAYER_STATS;

public class PlayerStats {

    public static void saveOrUpdatePlayerStats(DSLContext dsl, UUID uuid) {
        try {
            int day = NormalisedDate.getDays();
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            double balance = Nascraft.getEconomy().getBalance(player);
            double portfolio = PortfoliosManager.getInstance().getPortfolio(uuid).getValueOfDefaultCurrency();
            double debt = DebtManager.getInstance().getDebtOfPlayer(uuid);

            dsl.insertInto(PLAYER_STATS)
                    .set(PLAYER_STATS.UUID, uuid.toString())
                    .set(PLAYER_STATS.DAY, day)
                    .set(PLAYER_STATS.BALANCE, balance)
                    .set(PLAYER_STATS.PORTFOLIO, portfolio)
                    .set(PLAYER_STATS.DEBT, debt)
                    .onDuplicateKeyUpdate()
                    .set(PLAYER_STATS.BALANCE, balance)
                    .set(PLAYER_STATS.PORTFOLIO, portfolio)
                    .set(PLAYER_STATS.DEBT, debt)
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
    }

    public static List<PlayerStatsDTO> getAllPlayerStats(DSLContext dsl, UUID uuid) {
        List<PlayerStatsDTO> statsList = new ArrayList<>();
        try {
            var result = dsl.selectFrom(PLAYER_STATS)
                    .where(PLAYER_STATS.UUID.eq(uuid.toString()))
                    .orderBy(PLAYER_STATS.DAY.asc())
                    .fetch();

            for (var record : result) {
                statsList.add(new PlayerStatsDTO(
                        NormalisedDate.getDateFromDay(record.getDay()).getTime() / 1000,
                        record.getBalance(),
                        record.getPortfolio(),
                        record.getDebt()
                ));
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
        }
        return statsList;
    }

}
