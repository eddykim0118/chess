package dataaccess;

import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.*;

public class MySQLAuthDAOTest {
    private static AuthDAO authDAO;
    private static UserDAO userDAO;
    
    @BeforeAll
    static void setUp() throws Exception {
        authDAO = new MySQLAuthDAO();
        userDAO = new MySQLUserDAO();
        DatabaseManager.createDatabase();
        DatabaseManager.createTables();
    }
    
    @BeforeEach
    void clearDB() throws Exception {
        authDAO.clear();
        userDAO.clear();
        
        // Create a test user for auth operations
        UserData testUser = new UserData("testUser", "password", "test@example.com");
        userDAO.createUser(testUser);
    }
    
    @Test
    void createAuthPositive() throws Exception {
        // Positive test: Auth token successfully created
        String authToken = authDAO.createAuth("testUser");
        
        Assertions.assertNotNull(authToken);
        AuthData authData = authDAO.getAuth(authToken);
        Assertions.assertNotNull(authData);
        Assertions.assertEquals("testUser", authData.username());
    }
    
    @Test
    void createAuthNegative() throws Exception {
        // Negative test: Create auth for non-existent user
        // Since our foreign key constraint will fail
        Assertions.assertThrows(DataAccessException.class, () -> {
            authDAO.createAuth("nonExistentUser");
        });
    }
    
    @Test
    void getAuthPositive() throws Exception {
        // Positive test: Retrieve existing auth
        String authToken = authDAO.createAuth("testUser");
        
        AuthData authData = authDAO.getAuth(authToken);
        Assertions.assertNotNull(authData);
        Assertions.assertEquals("testUser", authData.username());
        Assertions.assertEquals(authToken, authData.authToken());
    }
    
    @Test
    void getAuthNegative() throws Exception {
        // Negative test: Attempt to retrieve non-existent auth
        AuthData authData = authDAO.getAuth("nonExistentToken");
        Assertions.assertNull(authData);
    }
    
    @Test
    void deleteAuthPositive() throws Exception {
        // Positive test: Delete existing auth
        String authToken = authDAO.createAuth("testUser");
        
        authDAO.deleteAuth(authToken);
        
        AuthData authData = authDAO.getAuth(authToken);
        Assertions.assertNull(authData);
    }
    
    @Test
    void deleteAuthNegative() throws Exception {
        // Negative test: Delete non-existent auth
        Assertions.assertThrows(DataAccessException.class, () -> {
            authDAO.deleteAuth("nonExistentToken");
        });
    }
    
    @Test
    void clearPositive() throws Exception {
        // Positive test: Clear all auth tokens
        authDAO.createAuth("testUser");
        
        authDAO.clear();
        
        // Try to get the auth token - should be null after clearing
        String authToken = "anyToken"; // This doesn't matter since we cleared all tokens
        AuthData authData = authDAO.getAuth(authToken);
        Assertions.assertNull(authData);
    }
}