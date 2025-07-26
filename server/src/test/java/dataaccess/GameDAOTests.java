package dataaccess;

import chess.ChessGame;
import model.GameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Collection;
import static org.junit.jupiter.api.Assertions.*;

public class GameDAOTests {
    private MySqlDataAccess dataAccess;

    @BeforeEach
    public void setUp() throws DataAccessException {
        dataAccess = new MySqlDataAccess();
        dataAccess.clear(); // Clear data before each test
    }

    @Test
    public void createGamePositive() throws DataAccessException {
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(0, null, null, "Test Game", game);

        int gameID = dataAccess.createGame(gameData);
        assertTrue(gameID > 0);

        GameData retrieved = dataAccess.getGame(gameID);
        assertNotNull(retrieved);
        assertEquals(gameID, retrieved.getGameID());
        assertEquals("Test Game", retrieved.getGameName());
        assertNull(retrieved.getWhiteUsername());
        assertNull(retrieved.getBlackUsername());
        assertNotNull(retrieved.getGame());
    }

    @Test
    public void createGameNegative() throws DataAccessException {
        // Test with null game data
        assertThrows(DataAccessException.class, () -> {
            dataAccess.createGame(null);
        });
    }

    @Test
    public void getGamePositive() throws DataAccessException {
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(0, "white", "black", "Test Game", game);

        int gameID = dataAccess.createGame(gameData);
        GameData retrieved = dataAccess.getGame(gameID);

        assertNotNull(retrieved);
        assertEquals(gameID, retrieved.getGameID());
        assertEquals("Test Game", retrieved.getGameName());
        assertEquals("white", retrieved.getWhiteUsername());
        assertEquals("black", retrieved.getBlackUsername());
    }

    @Test
    public void getGameNegative() throws DataAccessException {
        GameData retrieved = dataAccess.getGame(999);
        assertNull(retrieved);
    }

    @Test
    public void updateGamePositive() throws DataAccessException {
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(0, null, null, "Test Game", game);

        int gameID = dataAccess.createGame(gameData);
        GameData updatedGame = new GameData(gameID, "newWhite", "newBlack", "Test Game", game);

        dataAccess.updateGame(updatedGame);

        GameData retrieved = dataAccess.getGame(gameID);
        assertEquals("newWhite", retrieved.getWhiteUsername());
        assertEquals("newBlack", retrieved.getBlackUsername());
    }

    @Test
    public void updateGameNegative() throws DataAccessException {
        // Test updating non-existent game
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(999, "white", "black", "Test Game", game);

        assertThrows(DataAccessException.class, () -> {
            dataAccess.updateGame(gameData);
        });
    }

    @Test
    public void listGamesPositive() throws DataAccessException {
        ChessGame game1 = new ChessGame();
        ChessGame game2 = new ChessGame();

        dataAccess.createGame(new GameData(0, null, null, "Game 1", game1));
        dataAccess.createGame(new GameData(0, "white", null, "Game 2", game2));

        Collection<GameData> games = dataAccess.listGames();
        assertEquals(2, games.size());
    }

    @Test
    public void listGamesEmpty() throws DataAccessException {
        Collection<GameData> games = dataAccess.listGames();
        assertEquals(0, games.size());
    }

    @Test
    public void clearPositive() throws DataAccessException {
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(0, null, null, "Test Game", game);

        dataAccess.createGame(gameData);
        dataAccess.clear();

        Collection<GameData> games = dataAccess.listGames();
        assertEquals(0, games.size());
    }
}