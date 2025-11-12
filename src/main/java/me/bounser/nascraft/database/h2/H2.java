package me.bounser.nascraft.database.h2;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.AbstractRelationalDatabase;
import org.jooq.SQLDialect;

public class H2 extends AbstractRelationalDatabase {

    public H2(){
        super(
                String.format("jdbc:h2:file:%s;AUTO_SERVER=TRUE;MODE=MySQL", Nascraft.getInstance().getDataPath().resolve("nascraft").toAbsolutePath()),
                "org.h2.Driver",
                SQLDialect.H2,
                "",
                ""
        );
    }

    private static H2 INSTANCE;
    public static H2 getInstance() {
        if (INSTANCE == null) {
            return (INSTANCE = new H2());
        }
        return INSTANCE;
    }

}
