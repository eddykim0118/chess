package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.GameData;

import java.sql.Connection;
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
        try (Connection conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("DELETE FROM games")) {
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Cleared " + rowsAffected + " rows from games table");
        } catch (SQLException e) {
            System.err.println("Error in clear games: " + e.getMessage());
            throw new DataAccessException("Error clearing games table: " + e.getMessage());
        }
    }
    
    @Override
    public int createGame(String gameName) throws DataAccessException {
        if (gameName == null) {
            throw new DataAccessException("Error: bad request");
        }
        
        try (Connection conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("INSERT INTO games (gameName, game) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, gameName);
            // Create a new empty game
            ChessGame game = new ChessGame();
            String gameJson = gson.toJson(game);
            stmt.setString(2, gameJson);
            stmt.executeUpdate();
            
            try (var rs = stmt.getGeneratedKeys()) { 
                if (rs.next()) {
                    int gameId = rs.getInt(1);
                    System.out.println("Game created with ID: " + gameId);
                    return gameId;
                }
                throw new DataAccessException("Failed to create game - no ID returned");
            }
        } catch (SQLException e) {
            System.err.println("Error in createGame: " + e.getMessage());
            throw new DataAccessException("Error creating game: " + e.getMessage());
        }
    }
    
    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection();
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
            System.err.println("Error in getGame: " + e.getMessage());
            throw new DataAccessException("Error getting game: " + e.getMessage());
        }
    }
    
    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        var games = new ArrayList<GameData>();
        try (Connection conn = DatabaseManager.getConnection();
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
            System.out.println("Listed " + games.size() + " games");
            return games;
        } catch (SQLException e) {
            System.err.println("Error in listGames: " + e.getMessage());
            throw new DataAccessException("Error listing games: " + e.getMessage());
        }
    }
    
    @Override
    public void updateGame(int gameID, String whiteUsername, String blackUsername) throws DataAccessException {
        GameData game = getGame(gameID);
        if (game == null) {
            throw new DataAccessException("Error: bad request");
        }
        
        try (Connection conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("UPDATE games SET whiteUsername = ?, blackUsername = ? WHERE gameID = ?")) {
            // Use the existing values if the new values are null
            stmt.setString(1, whiteUsername != null ? whiteUsername : game.whiteUsername());
            stmt.setString(2, blackUsername != null ? blackUsername : game.blackUsername());
            stmt.setInt(3, gameID);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Game updated with ID: " + gameID);
            } else {
                throw new DataAccessException("Error: bad request");
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("foreign key constraint")) {
                throw new DataAccessException("Error: bad request");
            }
            System.err.println("Error in updateGame: " + e.getMessage());
            throw new DataAccessException("Error updating game: " + e.getMessage());
        }
    }
}