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
            // Load database properties
            try (var propStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("db.properties")) {
                if (propStream == null) {
                    System.err.println("ERROR: db.properties file not found!");
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
                
                System.out.println("Database configuration loaded:");
                System.out.println("URL: " + CONNECTION_URL);
                System.out.println("Database: " + DATABASE_NAME);
                System.out.println("User: " + USER);
            }
            // Log database configuration
            System.out.println("Database configuration loaded successfully");
        } catch (Exception ex) {
            System.err.println("CRITICAL ERROR: Unable to process db.properties: " + ex.getMessage());
            ex.printStackTrace();
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
            conn.close();
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
                // Create users table first (since it's referenced by other tables)
                String createUsers = "CREATE TABLE IF NOT EXISTS users (" +
                        "username VARCHAR(255) NOT NULL PRIMARY KEY, " +
                        "password VARCHAR(255) NOT NULL, " +
                        "email VARCHAR(255) NOT NULL)";
                stmt.executeUpdate(createUsers);
                
                // Then create auth table (which references users)
                String createAuth = "CREATE TABLE IF NOT EXISTS auth (" +
                        "authToken VARCHAR(255) NOT NULL PRIMARY KEY, " +
                        "username VARCHAR(255) NOT NULL, " +
                        "FOREIGN KEY (username) REFERENCES users(username))";
                stmt.executeUpdate(createAuth);
                
                // Finally create games table (which references users)
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
            // Add MySQL specific parameters for reliability
            String fullUrl = CONNECTION_URL + "/" + DATABASE_NAME + 
                    "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
            
            Connection conn = DriverManager.getConnection(fullUrl, USER, PASSWORD);
            conn.setAutoCommit(true);
            return conn;
        } catch (SQLException e) {
            System.err.println("Failed to get database connection: " + e.getMessage());
            throw new DataAccessException("Unable to connect to database: " + e.getMessage());
        }
    }

    /**
     * Counts the total number of rows across all database tables.
     */
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

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("Database connection test SUCCESSFUL");
            return true;
        } catch (Exception e) {
            System.err.println("Database connection test FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}