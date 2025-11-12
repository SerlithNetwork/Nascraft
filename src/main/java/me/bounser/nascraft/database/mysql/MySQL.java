package me.bounser.nascraft.database.mysql;

import me.bounser.nascraft.database.AbstractRelationalDatabase;
import org.jooq.SQLDialect;

public class MySQL extends AbstractRelationalDatabase {

    public MySQL(String host, int port, String database, String username, String password) {
        super(
                String.format("jdbc:mysql://%s:%s/%s", host, port, database),
                "com.mysql.cj.jdbc.Driver",
                SQLDialect.MYSQL,
                username,
                password
        );
    }

}
