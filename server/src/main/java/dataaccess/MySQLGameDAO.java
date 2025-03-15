package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.GameData;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

public class MySQLGameDAO implements GameDAO {
    private final Gson gson;
    
    public MySQLGameDAO() {
        this.gson = new Gson();
    }
    
    @Override
    public void clear() throws DataAccessException {

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("DELETE FROM games")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error clearing games table" + e.getMessage());
        }
    }
    
    @Override
    public int createGame(String gameName) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("INSERT INTO games (gameName, game) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, gameName);
            // Create a new empty game
            ChessGame game = new ChessGame();
            String gameJson = gson.toJson(game);
            stmt.setString(2, gameJson);
            stmt.executeUpdate();
            
            try (var rs = stmt.getGeneratedKeys()) { 
                if (rs.next()) {
                    return rs.getInt(1);
                }
                throw new DataAccessException("Failed to create game");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error creating game" + e.getMessage());
        }
    }
    
    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("SELECT * FROM games WHERE gameID = ?")) {
                stmt.setInt(1, gameID);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String whiteUsername = rs.getString("whiteUsername");
                    String blackUsername = rs.getString("blackUsername");
                    String gameName = rs.getString("gameName");
                    String gameJson = rs.getString("game");
                    ChessGame chessGame = gson.fromJson(gameJson, ChessGame.class);
                    return new GameData(gameID, whiteUsername, blackUsername, gameName, chessGame);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error getting game" + e.getMessage());
        }
    }
    
    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        var games = new ArrayList<GameData>();
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("SELECT * FROM games");
             var rs = stmt.executeQuery()) {
            while (rs.next()) {
                int gameID = rs.getInt("gameID");
                String whiteUsername = rs.getString("whiteUsername");
                String blackUsername = rs.getString("blackUsername");
                String gameName = rs.getString("gameName");
                String gameJson = rs.getString("game");
                ChessGame chessGame = gson.fromJson(gameJson, ChessGame.class);
                games.add(new GameData(gameID, whiteUsername, blackUsername, gameName, chessGame));
            }
            return games;
        } catch (SQLException e) {
            throw new DataAccessException("Error listing games" + e.getMessage());
        }
    }
    
    @Override
    public void updateGame(int gameID, String whiteUsername, String blackUsername) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
            var stmt = conn.prepareStatement("UPDATE games SET whiteUsername = ?, blackUsername = ? WHERE gameID = ?")) {
                stmt.setString(1, whiteUsername);
                stmt.setString(2, blackUsername);
                stmt.setInt(3, gameID);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DataAccessException("Game not found");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error updating game: " + e.getMessage());
        }
    }

}