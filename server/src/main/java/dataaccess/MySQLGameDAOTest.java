package dataaccess;

import org.junit.jupiter.api.*;
import chess.ChessGame;
import model.GameData;
import java.util.Collection;

public class MySQLGameDAOTest {
    private static GameDAO gameDAO;
    
    @BeforeAll
    static void setUp() throws Exception {
        gameDAO = new MySQLGameDAO();
        DatabaseManager.createDatabase();
        DatabaseManager.createTables();
    }
    
    @BeforeEach
    void clearDB() throws Exception {
        gameDAO.clear();
    }
    
    @Test
    void createGamePositive() throws Exception {
        // Positive test: Game successfully created
        int gameID = gameDAO.createGame("Test Game");
        
        GameData retrievedGame = gameDAO.getGame(gameID);
        Assertions.assertNotNull(retrievedGame);
        Assertions.assertEquals("Test Game", retrievedGame.gameName());
        Assertions.assertEquals(gameID, retrievedGame.gameID());
    }
    
    @Test
    void getGamePositive() throws Exception {
        // Positive test: Retrieve existing game
        int gameID = gameDAO.createGame("Test Game");
        
        GameData retrievedGame = gameDAO.getGame(gameID);
        Assertions.assertNotNull(retrievedGame);
        Assertions.assertEquals("Test Game", retrievedGame.gameName());
    }
    
    @Test
    void getGameNegative() throws Exception {
        // Negative test: Attempt to retrieve non-existent game
        GameData retrievedGame = gameDAO.getGame(999); // Assuming 999 is not a valid game ID
        Assertions.assertNull(retrievedGame);
    }
    
    @Test
    void listGamesPositive() throws Exception {
        // Positive test: List all games
        gameDAO.createGame("Test Game 1");
        gameDAO.createGame("Test Game 2");
        
        Collection<GameData> games = gameDAO.listGames();
        Assertions.assertEquals(2, games.size());
    }
    
    @Test
    void updateGamePositive() throws Exception {
        // Positive test: Update game
        int gameID = gameDAO.createGame("Test Game");
        GameData game = gameDAO.getGame(gameID);
        
        // Create a modified game with white player assigned
        GameData updatedGame = new GameData(
            gameID, 
            "whitePlayer", 
            null, 
            "Test Game", 
            game.game()
        );
        
        gameDAO.updateGame(updatedGame.gameID(), updatedGame.whiteUsername(), updatedGame.blackUsername());
        
        GameData retrievedGame = gameDAO.getGame(gameID);
        Assertions.assertEquals("whitePlayer", retrievedGame.whiteUsername());
    }
    
    @Test
    void updateGameNegative() throws Exception {
        // Negative test: Update non-existent game
        ChessGame chessGame = new ChessGame();
        GameData nonExistentGame = new GameData(999, null, null, "Non-existent Game", chessGame);
        
        Assertions.assertThrows(DataAccessException.class, () -> {
            gameDAO.updateGame(nonExistentGame.gameID(), nonExistentGame.whiteUsername(), nonExistentGame.blackUsername());
        });
    }
    
    @Test
    void clearPositive() throws Exception {
        // Positive test: Clear all games
        gameDAO.createGame("Test Game");
        
        gameDAO.clear();
        
        Collection<GameData> games = gameDAO.listGames();
        Assertions.assertEquals(0, games.size());
    }
}