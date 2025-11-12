
CREATE TABLE IF NOT EXISTS items (
    identifier VARCHAR(255) PRIMARY KEY,
    lastprice DOUBLE,
    lowest DOUBLE,
    highest DOUBLE,
    stock DOUBLE DEFAULT 0.0,
    taxes DOUBLE
);


CREATE TABLE IF NOT EXISTS prices_day (
    id INT AUTO_INCREMENT PRIMARY KEY,
    `day` INT,
    `date` VARCHAR(128),
    identifier VARCHAR(255),
    price DOUBLE,
    volume INT
);


CREATE TABLE IF NOT EXISTS prices_month (
    id INT AUTO_INCREMENT PRIMARY KEY,
    `day` INT NOT NULL,
    `date` VARCHAR(128) NOT NULL,
    identifier VARCHAR(255) NOT NULL,
    price DOUBLE NOT NULL,
    volume INT NOT NULL
);


CREATE TABLE IF NOT EXISTS prices_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    `day` INT,
    `date` VARCHAR(128) NOT NULL,
    identifier VARCHAR(255) NOT NULL,
    price DOUBLE,
    volume INT
);


CREATE TABLE IF NOT EXISTS portfolios (
    uuid VARCHAR(36),
    identifier VARCHAR(255),
    amount INT,
    PRIMARY KEY (uuid, identifier)
);


CREATE TABLE IF NOT EXISTS portfolios_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL,
    `day` INT,
    identifier VARCHAR(255),
    amount INT,
    contribution DOUBLE
);


CREATE TABLE IF NOT EXISTS portfolios_worth (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL,
    `day` INT,
    worth DOUBLE
);


CREATE TABLE IF NOT EXISTS capacities (
    uuid VARCHAR(36) PRIMARY KEY,
    capacity INT
);


CREATE TABLE IF NOT EXISTS discord_links (
    userid VARCHAR(18) NOT NULL,
    uuid VARCHAR(36) NOT NULL,
    nickname VARCHAR(255) NOT NULL,
    PRIMARY KEY (userid, uuid)
);


CREATE TABLE IF NOT EXISTS trade_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL,
    `day` INT NOT NULL,
    `date` VARCHAR(128) NOT NULL,
    identifier VARCHAR(255) NOT NULL,
    amount INT NOT NULL,
    `value` DOUBLE NOT NULL,
    buy BOOLEAN NOT NULL,
    discord BOOLEAN NOT NULL
);


CREATE TABLE IF NOT EXISTS cpi (
    `day` INT NOT NULL,
    `date` VARCHAR(128) NOT NULL,
    `value` DOUBLE NOT NULL,
    PRIMARY KEY (`day`, `date`)
);


CREATE TABLE IF NOT EXISTS alerts (
    `day` INT NOT NULL,
    userid VARCHAR(18) NOT NULL,
    identifier VARCHAR(255) NOT NULL,
    price DOUBLE NOT NULL,
    PRIMARY KEY (`day`, userid, identifier)
);


CREATE TABLE IF NOT EXISTS flows (
    `day` INT PRIMARY KEY,
    flow DOUBLE NOT NULL,
    taxes DOUBLE NOT NULL,
    operations INT NOT NULL
);


CREATE TABLE IF NOT EXISTS limit_orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    expiration VARCHAR(255) NOT NULL,
    uuid VARCHAR(36) NOT NULL,
    identifier VARCHAR(255) NOT NULL,
    `type` INT NOT NULL,
    price DOUBLE NOT NULL,
    to_complete INT NOT NULL,
    completed INT NOT NULL,
    cost INT NOT NULL
);


CREATE TABLE IF NOT EXISTS loans (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL,
    debt DOUBLE NOT NULL
);


CREATE TABLE IF NOT EXISTS interests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL,
    paid DOUBLE NOT NULL
);


CREATE TABLE IF NOT EXISTS user_names (
    uuid VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);


CREATE TABLE IF NOT EXISTS balances (
    uuid VARCHAR(36) PRIMARY KEY,
    balance DOUBLE NOT NULL
);


CREATE TABLE IF NOT EXISTS money_supply (
    `day` INT PRIMARY KEY,
    supply DOUBLE NOT NULL
);


CREATE TABLE IF NOT EXISTS web_credentials (
    `name` VARCHAR(255) NOT NULL,
    `pass` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`name`, `pass`)
);


CREATE TABLE IF NOT EXISTS player_stats (
    `day` INT,
    uuid VARCHAR(36) NOT NULL,
    balance DOUBLE NOT NULL,
    portfolio DOUBLE NOT NULL,
    debt DOUBLE NOT NULL,
    PRIMARY KEY (`day`, uuid)
);


CREATE TABLE IF NOT EXISTS discord (
    userid VARCHAR(18) NOT NULL,
    uuid VARCHAR(36) NOT NULL,
    nickname VARCHAR(255) NOT NULL,
    PRIMARY KEY (userid, uuid)
);

CREATE TABLE IF NOT EXISTS redis_credentials (
    username VARCHAR(255) NOT NULL,
    hash VARCHAR(255) NOT NULL,
    PRIMARY KEY (username, hash)
);
