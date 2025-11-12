package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.Price;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;

import static me.biquaternions.nascraft.schema.public_.Tables.ITEMS;

public class ItemProperties {

    public static void saveItem(DSLContext dsl, Item item) {
        Price price = item.getPrice();
        try {
            dsl.insertInto(ITEMS)
                    .set(ITEMS.IDENTIFIER, item.getIdentifier())
                    .set(ITEMS.LASTPRICE, price.getValue())
                    .set(ITEMS.LOWEST, price.getHistoricalLow())
                    .set(ITEMS.HIGHEST, price.getHistoricalHigh())
                    .set(ITEMS.STOCK, (double) price.getStock())
                    .set(ITEMS.TAXES, (double) item.getCollectedTaxes())
                    .onDuplicateKeyUpdate()
                    .set(ITEMS.LASTPRICE, price.getValue())
                    .set(ITEMS.LOWEST, price.getHistoricalLow())
                    .set(ITEMS.HIGHEST, price.getHistoricalHigh())
                    .set(ITEMS.STOCK, (double) price.getStock())
                    .set(ITEMS.TAXES, (double) item.getCollectedTaxes())
                    .execute();
        } catch (DataAccessException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    public static void retrieveItem(DSLContext dsl, Item item) {
        try {
            dsl.insertInto(ITEMS)
                    .set(ITEMS.IDENTIFIER, item.getIdentifier())
                    .set(ITEMS.LASTPRICE, (double) Config.getInstance().getInitialPrice(item.getIdentifier()))
                    .set(ITEMS.LOWEST, (double) Config.getInstance().getInitialPrice(item.getIdentifier()))
                    .set(ITEMS.HIGHEST, (double) Config.getInstance().getInitialPrice(item.getIdentifier()))
                    .set(ITEMS.STOCK, 0.0)
                    .set(ITEMS.TAXES, 0.0)
                    .onDuplicateKeyIgnore()
                    .execute();

            Record record = dsl.selectFrom(ITEMS)
                    .where(ITEMS.IDENTIFIER.eq(item.getIdentifier()))
                    .fetchOne();

            if (record != null) {
                item.getPrice().setStock(record.getValue(ITEMS.STOCK).floatValue());
                item.getPrice().setHistoricalHigh(record.getValue(ITEMS.HIGHEST).floatValue());
                item.getPrice().setHistoricalLow(record.getValue(ITEMS.LOWEST).floatValue());
                item.setCollectedTaxes(record.getValue(ITEMS.TAXES).floatValue());
            }

        } catch (DataAccessException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    public static float retrieveLastPrice(DSLContext dsl, Item item) {
        try {
            Record record = dsl.select(ITEMS.LASTPRICE)
                    .from(ITEMS)
                    .where(ITEMS.IDENTIFIER.eq(item.getIdentifier()))
                    .fetchOne();

            if (record != null) {
                return record.getValue(ITEMS.LASTPRICE).floatValue();
            }
            return Config.getInstance().getInitialPrice(item.getIdentifier());
        } catch (DataAccessException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
        return 0.0f;
    }

    public static void retrieveItems(DSLContext dsl) {
        try {
            var result = dsl.select(ITEMS.STOCK, ITEMS.IDENTIFIER)
                    .from(ITEMS)
                    .fetch();

            for (Record record : result) {
                Item item = MarketManager.getInstance().getItem(record.getValue(ITEMS.IDENTIFIER));
                if (item != null && item.isParent()) {
                    item.getPrice().setStock(record.getValue(ITEMS.STOCK).floatValue());
                }
            }
        } catch (DataAccessException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

}
