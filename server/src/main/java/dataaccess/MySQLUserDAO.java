package dataaccess;

import model.UserData;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.SQLException;
import java.sql.Connection;

public class MySQLUserDAO implements UserDAO {
    
    @Override
    public void clear() throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Temporarily disable foreign key checks
            try (var stmt = conn.prepareStatement("SET FOREIGN_KEY_CHECKS = 0")) {
                stmt.executeUpdate();
            }
            
            // Clear tables (order matters due to foreign key constraints)
            try (var authStmt = conn.prepareStatement("DELETE FROM auth");
                 var gamesStmt = conn.prepareStatement("DELETE FROM games");
                 var usersStmt = conn.prepareStatement("DELETE FROM users")) {
                authStmt.executeUpdate();
                gamesStmt.executeUpdate();
                usersStmt.executeUpdate();
            }
            
            // Re-enable foreign key checks
            try (var stmt = conn.prepareStatement("SET FOREIGN_KEY_CHECKS = 1")) {
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error in clear: " + e.getMessage());
            throw new DataAccessException("Error clearing database: " + e.getMessage());
        }
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        if (user == null || user.username() == null || user.password() == null || user.email() == null) {
            throw new DataAccessException("Error: bad request");
        }
        
        try (Connection conn = DatabaseManager.getConnection()) {
            // Use BCrypt to hash the password
            String hashedPassword = BCrypt.hashpw(user.password(), BCrypt.gensalt());
            
            try (var stmt = conn.prepareStatement("INSERT INTO users (username, password, email) VALUES (?, ?, ?)")) {
                stmt.setString(1, user.username());
                stmt.setString(2, hashedPassword);
                stmt.setString(3, user.email());
                stmt.executeUpdate();
                System.out.println("User created: " + user.username());
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                throw new DataAccessException("Error: already taken");
            }
            System.err.println("Error in createUser: " + e.getMessage());
            throw new DataAccessException("Error creating user: " + e.getMessage());
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
            System.err.println("Error in getUser: " + e.getMessage());
            throw new DataAccessException("Error getting user: " + e.getMessage());
        }
    }
}