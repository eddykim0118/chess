package dataaccess;

import model.User;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.SQLException;

public class MySqlUserDAO implements UserDAO {
    
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
    public void createUser(User user) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("INSERT INTO users (username, password, email) VALUES (?, ?, ?)")) {
                statement.setString(1, user.username());
                String hashPassword = BCrypt.hashpw(user.password(), BCrypt.gensalt());
                statement.setString(2, hashedPassword);
                statement.setSTring(3, user.email());
             } catch (SQLException e) {
                throw new DataAccessException("Error creating user", e);
             }
    }

    @Override
    public User getUser(String username) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?")) {
                stmt.setString(1, username);
                try (var rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new User(rs.getString("username"), rs.getString("password"), rs.getString("email"));
                    }
                    return null;
                }
             } catch (SQLException e) {
                throw new DataAccessException("Error getting user", e);
             }
    }
}