package dataaccess;

import model.UserData;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.SQLException;
import java.sql.Connection;

public class MySQLUserDAO implements UserDAO {
    
    @Override
    public void clear() throws DataAccessException {
        // Use a separate connection for each DAO operation
        try (Connection conn = DatabaseManager.getConnection()) {
            // Clear auth first (because of foreign key constraints)
            try (var stmt = conn.prepareStatement("DELETE FROM auth")) {
                stmt.executeUpdate();
            }
            
            // Then clear users
            try (var stmt = conn.prepareStatement("DELETE FROM users")) {
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error: failed to clear users: " + e.getMessage());
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
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry") && e.getMessage().contains("PRIMARY")) {
                throw new DataAccessException("Error: already taken");
            }
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
            throw new DataAccessException("Error getting user: " + e.getMessage());
        }
    }
}