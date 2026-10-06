package com.group49.support.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class Database {
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = Database.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (input == null) throw new IllegalStateException("database.properties was not found");
            PROPERTIES.load(input);
            Class.forName(PROPERTIES.getProperty("db.driver"));
        } catch (IOException | ClassNotFoundException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private Database() {}

    public static Connection getConnection() throws SQLException {
        String url = System.getenv().getOrDefault("CSMS_DB_URL", PROPERTIES.getProperty("db.url"));
        String username = System.getenv().getOrDefault("CSMS_DB_USERNAME", PROPERTIES.getProperty("db.username"));
        String password = System.getenv().getOrDefault("CSMS_DB_PASSWORD", PROPERTIES.getProperty("db.password"));
        return DriverManager.getConnection(url, username, password);
    }
}
