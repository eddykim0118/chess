package dataaccess;

import model.GameData;
import model.UserData;
import org.junit.jupiter.api.*;

import java.util.Collection;

public class MySQLGameDAOTest {
    private static GameDAO gameDAO;
    private static UserDAO userDAO; // Add this
    
    @BeforeAll
    static void setUp() throws Exception {
        gameDAO = new MySQLGameDAO();
        userDAO = new MySQLUserDAO(); // Add this
        DatabaseManager.createDatabase();
        DatabaseManager.createTables();
        
        // Create test user(s) for foreign key relationships
        try {
            UserData testUser = new UserData("whitePlayer", "password", "white@example.com");
            userDAO.createUser(testUser);
        } catch (DataAccessException e) {
            // Ignore if user already exists
        }
    }
    
    @BeforeEach
    void clearDB() throws Exception {
        gameDAO.clear();
        // Don't clear users or you'll break your foreign keys
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
        
        // Update game with white player - now "whitePlayer" exists in users table
        gameDAO.updateGame(gameID, "whitePlayer", null);
        
        GameData retrievedGame = gameDAO.getGame(gameID);
        Assertions.assertEquals("whitePlayer", retrievedGame.whiteUsername());
    }
    
    @Test
    void updateGameNegative() throws Exception {
        // Negative test: Update non-existent game
        int nonExistentGameID = 999;
        
        Assertions.assertThrows(DataAccessException.class, () -> {
            gameDAO.updateGame(nonExistentGameID, "whitePlayer", null);
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