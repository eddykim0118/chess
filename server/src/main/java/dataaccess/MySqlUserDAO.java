package dataaccess;

import model.UserData;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.SQLException;

public class MySQLUserDAO implements UserDAO {
    
    @Override
    public void clear() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("DELETE FROM users")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error: failed to clear users");
        }
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
            var stmt = conn.prepareStatement("INSERT INTO users (username, password, email) VALUES (?, ?, ?)")) {
                stmt.setString(1, user.username());
                String hashPassword = BCrypt.hashpw(user.password(), BCrypt.gensalt());
                stmt.setString(2, hashPassword);
                stmt.setString(3, user.email());
                stmt.executeUpdate(); // Execute the statement
            } catch (SQLException e) {
                throw new DataAccessException("Error creating user" + e.getMessage());
            }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?")) {
                stmt.setString(1, username);
                try (var rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new UserData(rs.getString("username"), rs.getString("password"), rs.getString("email"));
                    }
                    return null;
                }
             } catch (SQLException e) {
                throw new DataAccessException("Error getting user" + e.getMessage());
             }
    }
}