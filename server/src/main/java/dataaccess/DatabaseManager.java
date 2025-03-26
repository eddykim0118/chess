package dataaccess;

import java.sql.*;
import java.util.Properties;

public class DatabaseManager {
    private static final String DATABASE_NAME;
    private static final String USER;
    private static final String PASSWORD;
    private static final String CONNECTION_URL;

    static {
        try {
            try (var propStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("db.properties")) {
                if (propStream == null) {
                    throw new Exception("Unable to load db.properties");
                }
                Properties props = new Properties();
                props.load(propStream);
                DATABASE_NAME = props.getProperty("db.name");
                USER = props.getProperty("db.user");
                PASSWORD = props.getProperty("db.password");

                var host = props.getProperty("db.host");
                var port = Integer.parseInt(props.getProperty("db.port"));
                CONNECTION_URL = String.format("jdbc:mysql://%s:%d", host, port);
            }
        } catch (Exception ex) {
            throw new RuntimeException("unable to process db.properties. " + ex.getMessage());
        }
    }

    /**
     * Creates the database if it does not already exist
     */
    public static void createDatabase() throws DataAccessException {
        try {
            var statement = "CREATE DATABASE IF NOT EXISTS " + DATABASE_NAME;
            var conn = DriverManager.getConnection(CONNECTION_URL, USER, PASSWORD);
            try (var preparedStatement = conn.prepareStatement(statement)) {
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Unable to create database: " + e.getMessage());
        }
    }

    /**
     * Creates all required tables if they do not already exist
     */
    public static void createTables() throws DataAccessException {
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                String createUsers = "CREATE TABLE IF NOT EXISTS users (" +
                        "username VARCHAR(255) NOT NULL PRIMARY KEY, " +
                        "password VARCHAR(255) NOT NULL, " +
                        "email VARCHAR(255) NOT NULL)";
                stmt.executeUpdate(createUsers);
                
                String createAuth = "CREATE TABLE IF NOT EXISTS auth (" +
                        "authToken VARCHAR(255) NOT NULL PRIMARY KEY, " +
                        "username VARCHAR(255) NOT NULL, " +
                        "FOREIGN KEY (username) REFERENCES users(username))";
                stmt.executeUpdate(createAuth);
                
                String createGames = "CREATE TABLE IF NOT EXISTS games (" +
                        "gameID INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                        "whiteUsername VARCHAR(255), " +
                        "blackUsername VARCHAR(255), " +
                        "gameName VARCHAR(255) NOT NULL, " +
                        "game TEXT NOT NULL, " +
                        "FOREIGN KEY (whiteUsername) REFERENCES users(username), " +
                        "FOREIGN KEY (blackUsername) REFERENCES users(username))";
                stmt.executeUpdate(createGames);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error creating tables: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws DataAccessException {
        try {
            return DriverManager.getConnection(CONNECTION_URL + "/" + DATABASE_NAME, USER, PASSWORD);
        } catch (SQLException e) {
            throw new DataAccessException("Unable to connect to database: " + e.getMessage());
        }
    }

    /**
     * Counts the total number of rows across all database tables.
     * This method is intentionally kept for debugging and testing purposes.
     * 
     * @return The total number of rows in all tables
     */
    @SuppressWarnings("unused")
    public static int getDatabaseRowCount() {
        int count = 0;
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                ResultSet tables = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"});
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    if (tableName.equals("users") || tableName.equals("auth") || tableName.equals("games")) {
                        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName);
                        if (rs.next()) {
                            int tableCount = rs.getInt(1);
                            System.out.println(tableName + " has " + tableCount + " rows");
                            count += tableCount;
                        }
                    }
                }
            }
        } catch (SQLException | DataAccessException e) {
            System.err.println("Error counting rows: " + e.getMessage());
        }
        System.out.println("Total row count: " + count);
        return count;
    }
}