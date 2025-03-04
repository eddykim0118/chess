package service;

import dataaccess.*;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClearServiceTest {
    private ClearService clearService;
    private UserDAO userDAO;
    private GameDAO gameDAO;
    private AuthDAO authDAO;

    @BeforeEach
    public void setUp() {
        userDAO = new MemoryUserDAO();
        gameDAO = new MemoryGameDAO();
        authDAO = new MemoryAuthDAO();
        clearService = new ClearService(userDAO, gameDAO, authDAO);
    }

    @Test
    public void clearDatabasePositive() throws DataAccessException {
        // Create some data
        userDAO.createUser(new UserData("clearTestUser", "password", "test@example.com"));
        String authToken = authDAO.createAuth("clearTestUser");
        int gameID = gameDAO.createGame("Clear Test Game");
        
        // Clear the database
        clearService.clearDatabase();
        
        // Verify all data is cleared - this should throw exceptions which we're testing for
        assertThrows(DataAccessException.class, () -> userDAO.getUser("clearTestUser"));
        assertThrows(DataAccessException.class, () -> authDAO.getAuth(authToken));
        assertThrows(DataAccessException.class, () -> gameDAO.getGame(gameID));
    }
}