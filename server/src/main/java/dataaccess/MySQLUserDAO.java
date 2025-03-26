package dataaccess;

import model.UserData;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.SQLException;
import java.sql.Connection;

public class MySQLUserDAO implements UserDAO {
    
    @Override
    public void clear() throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Ensure we're using a transaction
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
            try (var stmt = conn.prepareStatement("DELETE FROM users")) {
                stmt.executeUpdate();
                conn.commit(); // Explicitly commit the transaction
            } catch (SQLException e) {
                conn.rollback(); // Rollback on error
                throw new DataAccessException("Error: failed to clear users: " + e.getMessage());
            } finally {
                // Restore original autoCommit setting
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database connection error: " + e.getMessage());
        }
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Ensure we're using a transaction
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
            try (var stmt = conn.prepareStatement("INSERT INTO users (username, password, email) VALUES (?, ?, ?)")) {
                stmt.setString(1, user.username());
                String hashPassword = BCrypt.hashpw(user.password(), BCrypt.gensalt());
                stmt.setString(2, hashPassword);
                stmt.setString(3, user.email());
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new DataAccessException("Failed to create user - no rows affected");
                }
                
                conn.commit(); // Explicitly commit the transaction
            } catch (SQLException e) {
                conn.rollback(); // Rollback on error
                if (e.getMessage().contains("Duplicate entry") && e.getMessage().contains("PRIMARY")) {
                    throw new DataAccessException("Error: username already taken");
                }
                throw new DataAccessException("Error creating user: " + e.getMessage());
            } finally {
                // Restore original autoCommit setting
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database connection error: " + e.getMessage());
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UserData(rs.getString("username"), rs.getString("password"), rs.getString("email"));
                }
                throw new DataAccessException("Error: unauthorized");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error getting user: " + e.getMessage());
        }
    }
}