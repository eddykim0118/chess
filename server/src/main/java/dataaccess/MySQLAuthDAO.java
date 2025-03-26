package dataaccess;

import model.AuthData;
import java.sql.SQLException;
import java.util.UUID;
import java.sql.PreparedStatement;

public class MySQLAuthDAO implements AuthDAO {
    
    @Override
    public void clear() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("DELETE FROM auth")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error clearing auth table: " + e.getMessage());
        }
    }
    
    @Override
    public String createAuth(String username) throws DataAccessException {
        if (username == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        
        try (var conn = DatabaseManager.getConnection()) {
            // Ensure autoCommit is enabled
            boolean originalAutoCommit = conn.getAutoCommit();
            if (!originalAutoCommit) {
                conn.setAutoCommit(true);
            }
            
            // First check if the username exists in the users table
            try (var checkStmt = conn.prepareStatement("SELECT username FROM users WHERE username = ?")) {
                checkStmt.setString(1, username);
                try (var rs = checkStmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new DataAccessException("Error: unauthorized");
                    }
                }
            }
            
            String authToken = UUID.randomUUID().toString();
            try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO auth (authToken, username) VALUES (?, ?)")) {
                stmt.setString(1, authToken);
                stmt.setString(2, username);
                stmt.executeUpdate();
                return authToken;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error creating auth token: " + e.getMessage());
        }
    }
    
    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        if (authToken == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        
        try (var conn = DatabaseManager.getConnection();
            var stmt = conn.prepareStatement("SELECT username FROM auth WHERE authToken = ?")) {
            stmt.setString(1, authToken);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String username = rs.getString("username");
                    return new AuthData(authToken, username);
                }
                // Return null for non-existent auth token rather than throwing an exception
                return null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error getting auth: " + e.getMessage());
        }
    }
    
    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        if (authToken == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("DELETE FROM auth WHERE authToken = ?")) {
            stmt.setString(1, authToken);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DataAccessException("Error: unauthorized");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error deleting auth token: " + e.getMessage());
        }
    }
}