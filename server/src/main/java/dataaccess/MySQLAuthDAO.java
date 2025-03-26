package dataaccess;

import model.AuthData;
import java.sql.SQLException;
import java.util.UUID;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class MySQLAuthDAO implements AuthDAO {
    
    @Override
    public void clear() throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Ensure we're using a transaction
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
            try (var stmt = conn.prepareStatement("DELETE FROM auth")) {
                stmt.executeUpdate();
                conn.commit(); // Explicitly commit the transaction
            } catch (SQLException e) {
                conn.rollback(); // Rollback on error
                throw new DataAccessException("Error clearing auth table: " + e.getMessage());
            } finally {
                // Restore original autoCommit setting
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database connection error: " + e.getMessage());
        }
    }
    
    @Override
    public String createAuth(String username) throws DataAccessException {
        if (username == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        
        try (Connection conn = DatabaseManager.getConnection()) {
            // Ensure we're using a transaction
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
            String authToken = UUID.randomUUID().toString();
            try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO auth (authToken, username) VALUES (?, ?)")) {
                stmt.setString(1, authToken);
                stmt.setString(2, username);
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new DataAccessException("Failed to create auth token - no rows affected");
                }
                
                conn.commit(); // Explicitly commit the transaction
                return authToken;
            } catch (SQLException e) {
                conn.rollback(); // Rollback on error
                throw new DataAccessException("Error creating auth token: " + e.getMessage());
            } finally {
                // Restore original autoCommit setting
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database connection error: " + e.getMessage());
        }
    }
    
    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        if (authToken == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        
        try (Connection conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("SELECT username FROM auth WHERE authToken = ?")) {
            stmt.setString(1, authToken);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String username = rs.getString("username");
                    return new AuthData(authToken, username);
                }
                throw new DataAccessException("Error: unauthorized");
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
        
        try (Connection conn = DatabaseManager.getConnection()) {
            // Ensure we're using a transaction
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
            try (var stmt = conn.prepareStatement("DELETE FROM auth WHERE authToken = ?")) {
                stmt.setString(1, authToken);
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new DataAccessException("Error: unauthorized");
                }
                
                conn.commit(); // Explicitly commit the transaction
            } catch (SQLException e) {
                conn.rollback(); // Rollback on error
                throw new DataAccessException("Error deleting auth token: " + e.getMessage());
            } finally {
                // Restore original autoCommit setting
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database connection error: " + e.getMessage());
        }
    }
}