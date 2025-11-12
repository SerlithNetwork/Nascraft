package me.bounser.nascraft.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.chart.cpi.CPIInstant;
import me.bounser.nascraft.database.commands.*;
import me.bounser.nascraft.database.commands.resources.DayInfo;
import me.bounser.nascraft.database.commands.resources.Trade;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.stats.Instant;
import me.bounser.nascraft.portfolio.Portfolio;
import me.bounser.nascraft.web.dto.PlayerStatsDTO;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

import static me.biquaternions.nascraft.schema.public_.Tables.*;

public abstract class AbstractRelationalDatabase implements Database {

    private static AbstractRelationalDatabase INSTANCE;

    private final HikariDataSource hikari;
    private final DSLContext dsl;

    protected AbstractRelationalDatabase(String url, String driver, SQLDialect dialect, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setDriverClassName(driver);
        config.setUsername(username);
        config.setPassword(password);

        this.hikari = new HikariDataSource(config);
        this.dsl = DSL.using(this.hikari, dialect);
    }

    public static AbstractRelationalDatabase getInstance() {
        return INSTANCE;
    }

    public static void setInstance(AbstractRelationalDatabase instance) {
        INSTANCE = instance;
    }

    public DSLContext getDsl() {
        return this.dsl;
    }

    @Override
    public void connect() {
        this.createTables();
    }

    @Override
    public void createTables() {
        this.dsl.createTableIfNotExists(ITEMS)
                .columns(ITEMS.fields())
                .execute();

        this.dsl.createTableIfNotExists(PRICES_DAY)
                .columns(PRICES_DAY.fields())
                .execute();

        this.dsl.createTableIfNotExists(PRICES_MONTH)
                .columns(PRICES_MONTH.fields())
                .execute();

        this.dsl.createTableIfNotExists(PRICES_HISTORY)
                .columns(PRICES_HISTORY.fields())
                .execute();

        this.dsl.createTableIfNotExists(PORTFOLIOS)
                .columns(PORTFOLIOS.fields())
                .execute();

        this.dsl.createTableIfNotExists(PORTFOLIOS_LOG)
                .columns(PORTFOLIOS_LOG.fields())
                .execute();

        this.dsl.createTableIfNotExists(PORTFOLIOS_WORTH)
                .columns(PORTFOLIOS_WORTH.fields())
                .execute();

        this.dsl.createTableIfNotExists(CAPACITIES)
                .columns(CAPACITIES.fields())
                .execute();

        this.dsl.createTableIfNotExists(DISCORD_LINKS)
                .columns(DISCORD_LINKS.fields())
                .execute();

        this.dsl.createTableIfNotExists(TRADE_LOG)
                .columns(TRADE_LOG.fields())
                .execute();

        this.dsl.createTableIfNotExists(CPI)
                .columns(CPI.fields())
                .execute();

        this.dsl.createTableIfNotExists(ALERTS)
                .columns(ALERTS.fields())
                .execute();

        this.dsl.createTableIfNotExists(FLOWS)
                .columns(FLOWS.fields())
                .execute();

        this.dsl.createTableIfNotExists(LIMIT_ORDERS)
                .columns(LIMIT_ORDERS.fields())
                .execute();

        this.dsl.createTableIfNotExists(LOANS)
                .columns(LOANS.fields())
                .execute();

        this.dsl.createTableIfNotExists(INTERESTS)
                .columns(INTERESTS.fields())
                .execute();

        this.dsl.createTableIfNotExists(USER_NAMES)
                .columns(USER_NAMES.fields())
                .execute();

        this.dsl.createTableIfNotExists(BALANCES)
                .columns(BALANCES.fields())
                .execute();

        this.dsl.createTableIfNotExists(MONEY_SUPPLY)
                .columns(MONEY_SUPPLY.fields())
                .execute();

        this.dsl.createTableIfNotExists(WEB_CREDENTIALS)
                .columns(WEB_CREDENTIALS.fields())
                .execute();

        this.dsl.createTableIfNotExists(PLAYER_STATS)
                .columns(PLAYER_STATS.fields())
                .execute();

        this.dsl.createTableIfNotExists(DISCORD)
                .columns(DISCORD.fields())
                .execute();
    }

    @Override
    public void disconnect() {
        if (!isConnected() || this.hikari.isClosed()) {
            return;
        }
        this.hikari.close();
    }

    @Override
    public boolean isConnected() {
        return this.hikari.isRunning();
    }

    @Override
    public void saveEverything() {
        for (Item item : MarketManager.getInstance().getAllParentItems()) {
            ItemProperties.saveItem(this.getDsl(), item);
        }
    }

    @Override
    public void saveLink(String userId, UUID uuid, String nickname) {
        DiscordLink.saveLink(this.getDsl(), userId, uuid, nickname);
    }

    @Override
    public void removeLink(String userId) {
        DiscordLink.removeLink(this.getDsl(), userId);
    }

    @Override
    public UUID getUUID(String userId) {
        return DiscordLink.getUUID(this.getDsl(), userId);
    }

    @Override
    public String getNickname(String userId) {
        return DiscordLink.getNickname(this.getDsl(), userId);
    }

    @Override
    public String getUserId(UUID uuid) {
        return DiscordLink.getUserId(this.getDsl(), uuid);
    }

    @Override
    public void saveDayPrice(Item item, Instant instant) {
        HistorialData.saveDayPrice(this.getDsl(), item, instant);
    }

    @Override
    public void saveMonthPrice(Item item, Instant instant) {
        HistorialData.saveMonthPrice(this.getDsl(), item, instant);
    }

    @Override
    public void saveHistoryPrices(Item item, Instant instant) {
        HistorialData.saveHistoryPrices(this.getDsl(), item, instant);
    }

    @Override
    public List<Instant> getDayPrices(Item item) {
        return HistorialData.getDayPrices(this.getDsl(), item);
    }

    @Override
    public List<Instant> getMonthPrices(Item item) {
        return HistorialData.getMonthPrices(this.getDsl(), item);
    }

    @Override
    public List<Instant> getYearPrices(Item item) {
        return HistorialData.getYearPrices(this.getDsl(), item);
    }

    @Override
    public List<Instant> getAllPrices(Item item) {
        return HistorialData.getAllPrices(this.getDsl(), item);
    }

    @Override
    public Double getPriceOfDay(String identifier, int day) {
        return HistorialData.getPriceOfDay(this.getDsl(), identifier, day);
    }

    @Override
    public void saveItem(Item item) {
        ItemProperties.saveItem(this.getDsl(), item);
    }

    @Override
    public void retrieveItem(Item item) {
        ItemProperties.retrieveItem(this.getDsl(), item);
    }

    @Override
    public void retrieveItems() {
        ItemProperties.retrieveItems(this.getDsl());
    }

    @Override
    public float retrieveLastPrice(Item item) {
        return ItemProperties.retrieveLastPrice(this.getDsl(), item);
    }

    @Override
    public void saveTrade(Trade trade) {
        TradesLog.saveTrade(this.getDsl(), trade);
    }

    @Override
    public List<Trade> retrieveTrades(UUID uuid, int offset, int limit) {
        return TradesLog.retrieveTrades(this.getDsl(), uuid, offset, limit);
    }

    @Override
    public List<Trade> retrieveTrades(UUID uuid, Item item, int offset, int limit) {
        return TradesLog.retrieveTrades(this.getDsl(), uuid, item, offset, limit);
    }

    @Override
    public List<Trade> retrieveTrades(Item item, int offset, int limit) {
        return TradesLog.retrieveTrades(this.getDsl(), item, offset, limit);
    }

    @Override
    public List<Trade> retrieveTrades(int offset, int limit) {
        return TradesLog.retrieveLastTrades(this.getDsl(), offset, limit);
    }

    @Override
    public void purgeHistory() {
        TradesLog.purgeHistory(this.getDsl());
    }

    @Override
    public void updateItemPortfolio(UUID uuid, Item item, int quantity) {
        Portfolios.updateItemPortfolio(this.getDsl(), uuid, item, quantity);
    }

    @Override
    public void removeItemPortfolio(UUID uuid, Item item) {
        Portfolios.removeItemPortfolio(this.getDsl(), uuid, item);
    }

    @Override
    public void clearPortfolio(UUID uuid) {
        Portfolios.clearPortfolio(this.getDsl(), uuid);
    }

    @Override
    public void updateCapacity(UUID uuid, int capacity) {
        Portfolios.updateCapacity(this.getDsl(), uuid, capacity);
    }

    @Override
    public LinkedHashMap<Item, Integer> retrievePortfolio(UUID uuid) {
        return Portfolios.retrievePortfolio(this.getDsl(), uuid);
    }

    @Override
    public int retrieveCapacity(UUID uuid) {
        return Portfolios.retrieveCapacity(this.getDsl(), uuid);
    }

    @Override
    public void logContribution(UUID uuid, Item item, int amount) {
        PortfoliosLog.logContribution(this.getDsl(), uuid, item, amount);
    }

    @Override
    public void logWithdraw(UUID uuid, Item item, int amount) {
        PortfoliosLog.logWithdraw(this.getDsl(), uuid, item, amount);
    }

    @Override
    public HashMap<Integer, Double> getContributionChangeEachDay(UUID uuid) {
        return PortfoliosLog.getContributionChangeEachDay(this.getDsl(), uuid);
    }

    @Override
    public HashMap<Integer, HashMap<String, Integer>> getCompositionEachDay(UUID uuid) {
        return PortfoliosLog.getCompositionEachDay(this.getDsl(), uuid);
    }

    @Override
    public int getFirstDay(UUID uuid) {
        return PortfoliosLog.getFirstDay(this.getDsl(), uuid);
    }

    @Override
    public void increaseDebt(UUID uuid, Double debt) {
        Debt.increaseDebt(this.getDsl(), uuid, debt);
    }

    @Override
    public void decreaseDebt(UUID uuid, Double debt) {
        Debt.decreaseDebt(this.getDsl(), uuid, debt);
    }

    @Override
    public double getDebt(UUID uuid) {
        return Debt.getDebt(this.getDsl(), uuid);
    }

    @Override
    public HashMap<UUID, Double> getUUIDAndDebt() {
        return Debt.getUUIDAndDebt(this.getDsl());
    }

    @Override
    public void addInterestPaid(UUID uuid, Double interest) {
        Debt.addInterestPaid(this.getDsl(), uuid, interest);
    }

    @Override
    public HashMap<UUID, Double> getUUIDAndInterestsPaid() {
        return Debt.getUUIDAndInterestsPaid(this.getDsl());
    }

    @Override
    public double getInterestsPaid(UUID uuid) {
        return Debt.getInterestsPaid(this.getDsl(), uuid);
    }

    @Override
    public double getAllOutstandingDebt() {
        return Debt.getAllOutstandingDebt(this.getDsl());
    }

    @Override
    public double getAllInterestsPaid() {
        return Debt.getAllInterestsPaid(this.getDsl());
    }

    @Override
    public void saveOrUpdateWorth(UUID uuid, int day, double worth) {
        PortfoliosWorth.saveOrUpdateWorth(this.getDsl(), uuid, day, worth);
    }

    @Override
    public void saveOrUpdateWorthToday(UUID uuid, double worth) {
        PortfoliosWorth.saveOrUpdateWorthToday(this.getDsl(), uuid, worth);
    }

    @Override
    public HashMap<UUID, Portfolio> getTopWorth(int n) {
        return PortfoliosWorth.getTopWorth(this.getDsl(), n);
    }

    @Override
    public double getLatestWorth(UUID uuid) {
        return PortfoliosWorth.getLatestWorth(this.getDsl(), uuid);
    }

    @Override
    public void saveCPIValue(float indexValue) {
        Statistics.saveCPI(this.getDsl(), indexValue);
    }

    @Override
    public List<CPIInstant> getCPIHistory() {
        return Statistics.getAllCPI(this.getDsl());
    }

    @Override
    public List<Instant> getPriceAgainstCPI(Item item) {
        return Statistics.getPriceAgainstCPI(this.getDsl(), item);
    }

    @Override
    public void addTransaction(double newFlow, double effectiveTaxes) {
        Statistics.addTransaction(this.getDsl(), newFlow, effectiveTaxes);
    }

    @Override
    public List<DayInfo> getDayInfos() {
        return Statistics.getDayInfos(this.getDsl());
    }

    @Override
    public double getAllTaxesCollected() {
        return Statistics.getAllTaxesCollected(this.getDsl());
    }

    @Override
    public void addAlert(String userid, Item item, double price) {
        Alerts.addAlert(this.getDsl(), userid, item, price);
    }

    @Override
    public void removeAlert(String userid, Item item) {
        Alerts.removeAlert(this.getDsl(), userid, item);
    }

    @Override
    public void retrieveAlerts() {
        Alerts.retrieveAlerts(this.getDsl());
    }

    @Override
    public void removeAllAlerts(String userid) {
        Alerts.removeAllAlerts(this.getDsl(), userid);
    }

    @Override
    public void purgeAlerts() {
        Alerts.purgeAlerts(this.getDsl());
    }

    @Override
    public void addLimitOrder(UUID uuid, LocalDateTime expiration, Item item, int type, double price, int amount) {
        LimitOrders.addLimitOrder(this.getDsl(), uuid, expiration, item, type, price, amount);
    }

    @Override
    public void updateLimitOrder(UUID uuid, Item item, int completed, double cost) {
        LimitOrders.updateLimitOrder(this.getDsl(), uuid, item, completed, cost);
    }

    @Override
    public void removeLimitOrder(String uuid, String identifier) {
        LimitOrders.removeLimitOrder(this.getDsl(), uuid, identifier);
    }

    @Override
    public void retrieveLimitOrders() {
        LimitOrders.retrieveLimitOrders(this.getDsl());
    }

    @Override
    public String getNameByUUID(UUID uuid) {
        return UserNames.getNameByUUID(this.getDsl(), uuid);
    }

    @Override
    public UUID getUUIDbyName(String name) {
        return UserNames.getUUIDbyName(this.getDsl(), name);
    }

    @Override
    public void saveOrUpdateName(UUID uuid, String name) {
        UserNames.saveOrUpdateNick(this.getDsl(), uuid, name);
    }

    @Override
    public void updateBalance(UUID uuid) {
        Balances.updateBalance(this.getDsl(), uuid);
    }

    @Override
    public Map<Integer, Double> getMoneySupplyHistory() {
        return Balances.getMoneySupplyHistory(this.getDsl());
    }

    private static class Credentials {

        public static void storeCredentials(DSLContext dsl, String userName, String hash) {
            try {
                dsl.insertInto(REDIS_CREDENTIALS)
                        .set(REDIS_CREDENTIALS.USERNAME, userName)
                        .set(REDIS_CREDENTIALS.HASH, hash)
                        .execute();
            } catch (DataAccessException e) {
                Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
            }
        }

        public static String retrieveHash(DSLContext dsl, String userName) {
            try {
                var record = dsl.select(REDIS_CREDENTIALS.HASH)
                        .from(REDIS_CREDENTIALS)
                        .where(REDIS_CREDENTIALS.USERNAME.eq(userName))
                        .fetchOne();

                if (record != null) {
                    return record.get(REDIS_CREDENTIALS.HASH);
                }
            } catch (DataAccessException e) {
                Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
            }
            return null;
        }

        public static void clearUserCredentials(DSLContext dsl, String userName) {
            try {
                dsl.deleteFrom(REDIS_CREDENTIALS)
                        .where(REDIS_CREDENTIALS.USERNAME.eq(userName))
                        .execute();
            } catch (DataAccessException e) {
                Nascraft.getInstance().getSLF4JLogger().warn(e.getMessage(), e);
            }
        }
    }

    @Override
    public void storeCredentials(String userName, String hash) {
        AbstractRelationalDatabase.Credentials.storeCredentials(this.getDsl(), userName, hash);
    }

    @Override
    public String retrieveHash(String userName) {
        return AbstractRelationalDatabase.Credentials.retrieveHash(this.getDsl(), userName);
    }

    @Override
    public void clearUserCredentials(String userName) {
        AbstractRelationalDatabase.Credentials.clearUserCredentials(this.getDsl(), userName);
    }

    @Override
    public void saveOrUpdatePlayerStats(UUID uuid) {
        PlayerStats.saveOrUpdatePlayerStats(this.getDsl(), uuid);
    }

    @Override
    public List<PlayerStatsDTO> getAllPlayerStats(UUID uuid) {
        return PlayerStats.getAllPlayerStats(this.getDsl(), uuid);
    }

    @Override
    public void saveDiscordLink(UUID uuid, String userid, String nickname) {
        Discord.saveDiscordLink(this.getDsl(), uuid, userid, nickname);
    }

    @Override
    public void removeDiscordLink(UUID uuid) {
        Discord.removeLink(this.getDsl(), uuid);
    }

    @Override
    public String getDiscordUserId(UUID uuid) {
        return Discord.getDiscordUserId(this.getDsl(), uuid);
    }

    @Override
    public UUID getUUIDFromUserid(String userid) {
        return Discord.getUUIDFromUserid(this.getDsl(), userid);
    }

    @Override
    public String getNicknameFromUserId(String userid) {
        return Discord.getNicknameFromUserId(this.getDsl(), userid);
    }

}
