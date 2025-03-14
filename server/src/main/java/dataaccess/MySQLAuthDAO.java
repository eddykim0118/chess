package dataaccess;

import model.AuthData;
import java.sql.SQLException;
import java.util.UUID;

public class MySQLAuthDAO implements AuthDAO {
    
    @Override
    public void clear() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var statement = conn.prepareStatement("DELETE FROM auth")) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error clearing auth table" + e.getMessage());
        }
    }
    
    @Override
    public String createAuth(String username) throws DataAccessException {
        String authToken = UUID.randomUUID().toString();
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("INSERT INTO auth (authToken, username) VALUES (?, ?)")) {
                stmt.setString(1, authToken);
                stmt.setString(2, username);
                stmt.executeUpdate();
            return authToken;
        } catch (SQLException e) {
            throw new DataAccessException("Error creating auth token" + e.getMessage());
        }
    }
    
    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("SELECT * FROM auth WHERE authToken = ?")) {
                stmt.setString(1, authToken);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String username = rs.getString("username");
                    return new AuthData(authToken, username);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error getting auth data" + e.getMessage());
        }
    }
    
    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("DELETE FROM auth WHERE authToken = ?")) {
                stmt.setString(1, authToken);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DataAccessException("Auth token not found");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error deleting auth token" + e.getMessage());
        }
    }
}