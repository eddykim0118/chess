package dataaccess;

import model.AuthData;
import java.sql.SQLException;
import java.util.UUID;
import java.sql.Connection;

public class MySQLAuthDAO implements AuthDAO {
    
    @Override
    public void clear() throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("DELETE FROM auth")) {
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Cleared " + rowsAffected + " rows from auth table");
        } catch (SQLException e) {
            System.err.println("Error in clear auth: " + e.getMessage());
            throw new DataAccessException("Error clearing auth table: " + e.getMessage());
        }
    }
    
    @Override
    public String createAuth(String username) throws DataAccessException {
        if (username == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        
        try (Connection conn = DatabaseManager.getConnection()) {
            // First check if the username exists in the users table
            try (var checkStmt = conn.prepareStatement("SELECT username FROM users WHERE username = ?")) {
                checkStmt.setString(1, username);
                try (var rs = checkStmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new DataAccessException("Error: unauthorized");
                    }
                }
            }
            
            // Generate a unique auth token
            String authToken = UUID.randomUUID().toString();
            
            try (var stmt = conn.prepareStatement("INSERT INTO auth (authToken, username) VALUES (?, ?)")) {
                stmt.setString(1, authToken);
                stmt.setString(2, username);
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    System.out.println("Auth token created for user: " + username);
                    return authToken;
                } else {
                    throw new DataAccessException("Failed to create auth token");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error in createAuth: " + e.getMessage());
            throw new DataAccessException("Error creating auth token: " + e.getMessage());
        }
    }
    
    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        if (authToken == null) {
            return null; // Return null instead of throwing exception
        }
        
        try (Connection conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("SELECT username FROM auth WHERE authToken = ?")) {
            stmt.setString(1, authToken);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String username = rs.getString("username");
                    return new AuthData(authToken, username);
                }
                return null; // Return null for non-existent tokens
            }
        } catch (SQLException e) {
            System.err.println("Error in getAuth: " + e.getMessage());
            throw new DataAccessException("Error getting auth: " + e.getMessage());
        }
    }
    
    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        if (authToken == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        
        try (Connection conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("DELETE FROM auth WHERE authToken = ?")) {
            stmt.setString(1, authToken);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DataAccessException("Error: unauthorized");
            }
            System.out.println("Auth token deleted: " + authToken);
        } catch (SQLException e) {
            System.err.println("Error in deleteAuth: " + e.getMessage());
            throw new DataAccessException("Error deleting auth token: " + e.getMessage());
        }
    }
}