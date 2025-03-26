package dataaccess;

import model.UserData;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.Connection;
import java.sql.SQLException;

public class MySQLUserDAO implements UserDAO {
    
    @Override
    public void clear() throws DataAccessException {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            
            try (var stmt = conn.prepareStatement("DELETE FROM auth");
                 var stmt2 = conn.prepareStatement("DELETE FROM users")) {
                // Clear auth first because of foreign key constraints
                stmt.executeUpdate();
                stmt2.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                if (conn != null) {
                    conn.rollback();
                }
                throw new DataAccessException("Error: failed to clear users: " + e.getMessage());
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
    public void createUser(UserData user) throws DataAccessException {
        if (user == null || user.username() == null || user.password() == null || user.email() == null) {
            throw new DataAccessException("Error: bad request");
        }
        
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            
            try (var stmt = conn.prepareStatement("INSERT INTO users (username, password, email) VALUES (?, ?, ?)")) {
                stmt.setString(1, user.username());
                String hashPassword = BCrypt.hashpw(user.password(), BCrypt.gensalt());
                stmt.setString(2, hashPassword);
                stmt.setString(3, user.email());
                stmt.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                if (conn != null) {
                    conn.rollback();
                }
                if (e.getMessage().contains("Duplicate entry") && e.getMessage().contains("PRIMARY")) {
                    throw new DataAccessException("Error: already taken");
                }
                throw new DataAccessException("Error creating user: " + e.getMessage());
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
    public UserData getUser(String username) throws DataAccessException {
        if (username == null) {
            throw new DataAccessException("Error: bad request");
        }
        
        try (Connection conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UserData(rs.getString("username"), rs.getString("password"), rs.getString("email"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error getting user: " + e.getMessage());
        }
    }
}