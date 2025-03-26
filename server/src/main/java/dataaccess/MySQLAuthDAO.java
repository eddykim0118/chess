package dataaccess;

import model.AuthData;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

public class MySQLAuthDAO implements AuthDAO {
    
    @Override
    public void clear() throws DataAccessException {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            
            try (var stmt = conn.prepareStatement("DELETE FROM auth")) {
                stmt.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                if (conn != null) {
                    conn.rollback();
                }
                throw new DataAccessException("Error clearing auth table: " + e.getMessage());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database connection error: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Failed to close connection: " + e.getMessage());
                }
            }
        }
    }
    
    @Override
    public String createAuth(String username) throws DataAccessException {
        if (username == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            
            // First check if the username exists
            try (var checkStmt = conn.prepareStatement("SELECT username FROM users WHERE username = ?")) {
                checkStmt.setString(1, username);
                try (var rs = checkStmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new DataAccessException("Error: unauthorized");
                    }
                }
                
                String authToken = UUID.randomUUID().toString();
                try (var stmt = conn.prepareStatement("INSERT INTO auth (authToken, username) VALUES (?, ?)")) {
                    stmt.setString(1, authToken);
                    stmt.setString(2, username);
                    stmt.executeUpdate();
                    conn.commit();
                    return authToken;
                }
            } catch (SQLException e) {
                if (conn != null) {
                    conn.rollback();
                }
                throw new DataAccessException("Error creating auth token: " + e.getMessage());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database connection error: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Failed to close connection: " + e.getMessage());
                }
            }
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
        
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            
            try (var stmt = conn.prepareStatement("DELETE FROM auth WHERE authToken = ?")) {
                stmt.setString(1, authToken);
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new DataAccessException("Error: unauthorized");
                }
                conn.commit();
            } catch (SQLException e) {
                if (conn != null) {
                    conn.rollback();
                }
                throw new DataAccessException("Error deleting auth token: " + e.getMessage());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database connection error: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Failed to close connection: " + e.getMessage());
                }
            }
        }
    }
}