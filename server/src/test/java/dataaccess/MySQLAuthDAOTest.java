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
        
        UserData testUser = new UserData("testUser", "password", "test@example.com");
        userDAO.createUser(testUser);
    }
    
    @Test
    void createAuthPositive() throws Exception {
        String authToken = authDAO.createAuth("testUser");
        
        Assertions.assertNotNull(authToken);
        AuthData authData = authDAO.getAuth(authToken);
        Assertions.assertNotNull(authData);
        Assertions.assertEquals("testUser", authData.username());
    }
    
    @Test
    void createAuthNegative() throws Exception {
        Assertions.assertThrows(DataAccessException.class, () -> {
            authDAO.createAuth("nonExistentUser");
        });
    }
    
    @Test
    void getAuthPositive() throws Exception {
        String authToken = authDAO.createAuth("testUser");
        
        AuthData authData = authDAO.getAuth(authToken);
        Assertions.assertNotNull(authData);
        Assertions.assertEquals("testUser", authData.username());
        Assertions.assertEquals(authToken, authData.authToken());
    }
    
    @Test
    void getAuthNegative() throws Exception {
        AuthData authData = authDAO.getAuth("nonExistentToken");
        Assertions.assertNull(authData);
    }
    
    @Test
    void deleteAuthPositive() throws Exception {
        String authToken = authDAO.createAuth("testUser");
        
        authDAO.deleteAuth(authToken);
        
        AuthData authData = authDAO.getAuth(authToken);
        Assertions.assertNull(authData);
    }
    
    @Test
    void deleteAuthNegative() throws Exception {
        Assertions.assertThrows(DataAccessException.class, () -> {
            authDAO.deleteAuth("nonExistentToken");
        });
    }
    
    @Test
    void clearPositive() throws Exception {
        authDAO.createAuth("testUser");
        
        authDAO.clear();
        
        String authToken = "anyToken";
        AuthData authData = authDAO.getAuth(authToken);
        Assertions.assertNull(authData);
    }
}