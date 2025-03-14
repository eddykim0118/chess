package service;

import dataaccess.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.requests.CreateGameRequest;
import service.requests.JoinGameRequest;
import service.results.CreateGameResult;
import service.results.ListGamesResult;

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTest {
    private GameService gameService;
    private GameDAO gameDAO;
    private AuthDAO authDAO;
    private String authToken;

    @BeforeEach
    public void setUp() throws DataAccessException {
        gameDAO = new MemoryGameDAO();
        authDAO = new MemoryAuthDAO();
        gameService = new GameService(gameDAO, authDAO);
        
        // Create a test auth token
        authToken = authDAO.createAuth("testUser");
    }

    @Test
    public void listGamesPositive() throws DataAccessException {
        // Create a couple of games
        gameDAO.createGame("Game 1");
        gameDAO.createGame("Game 2");
        
        // Test listing games
        ListGamesResult result = gameService.listGames(authToken);
        
        assertNotNull(result);
        assertEquals(2, result.games().size());
    }

    @Test
    public void listGamesNegative() {
        // Test with invalid auth token
        assertThrows(DataAccessException.class, () -> gameService.listGames("invalidauthtoken"));
    }

    @Test
    public void createGamePositive() throws DataAccessException {
        // Test successful game creation
        CreateGameRequest request = new CreateGameRequest("New Game");
        CreateGameResult result = gameService.createGame(authToken, request);
        
        assertNotNull(result);
        assertTrue(result.gameID() > 0);
    }

    @Test
    public void createGameNegative() {
        // Test with null game name
        CreateGameRequest badRequest = new CreateGameRequest(null);
        assertThrows(DataAccessException.class, () -> gameService.createGame(authToken, badRequest));
    }

    @Test
    public void joinGamePositive() throws DataAccessException {
        // Create a game
        int gameID = gameDAO.createGame("Join Test Game");
        
        // Test joining as WHITE
        JoinGameRequest whiteRequest = new JoinGameRequest("WHITE", gameID);
        assertDoesNotThrow(() -> gameService.joinGame(authToken, whiteRequest));
    }

    @Test
    public void joinGameNegative() throws DataAccessException {
        // Create a game
        int gameID = gameDAO.createGame("Join Test Game");
        
        // Join as WHITE
        JoinGameRequest whiteRequest = new JoinGameRequest("WHITE", gameID);
        gameService.joinGame(authToken, whiteRequest);
        
        // Try to join as WHITE again (should fail)
        String authToken2 = authDAO.createAuth("testUser2");
        assertThrows(DataAccessException.class, () -> gameService.joinGame(authToken2, whiteRequest));
    }

    @Test
    public void joinGameInvalidColor() {
        // Create a game first
        try {
            int gameID = gameDAO.createGame("Invalid Color Test Game");
            
            // Try to join with an invalid color
            JoinGameRequest invalidRequest = new JoinGameRequest("PURPLE", gameID);
            
            // This should throw an exception due to invalid color
            assertThrows(DataAccessException.class, () -> gameService.joinGame(authToken, invalidRequest));
        } catch (DataAccessException e) {
            fail("Setup failed: " + e.getMessage());
        }
    }
}