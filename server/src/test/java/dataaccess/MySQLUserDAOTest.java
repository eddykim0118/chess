package dataaccess;

import model.UserData;
import org.junit.jupiter.api.*;

public class MySQLUserDAOTest {
    private static UserDAO userDAO;
    
    @BeforeAll
    static void setUp() throws Exception {
        userDAO = new MySQLUserDAO();
        DatabaseManager.createDatabase();
        DatabaseManager.createTables();
    }
    
    @BeforeEach
    void clearDB() throws Exception {
        userDAO.clear();
    }
    
    @Test
    void createUserPositive() throws Exception {
        UserData user = new UserData("testUser", "password", "test@example.com");
        userDAO.createUser(user);
        
        UserData retrievedUser = userDAO.getUser("testUser");
        Assertions.assertNotNull(retrievedUser);
        Assertions.assertEquals("testUser", retrievedUser.username());
    }
    
    @Test
    void createUserNegative() throws Exception {
        UserData user = new UserData("testUser", "password", "test@example.com");
        userDAO.createUser(user);
        
        Assertions.assertThrows(DataAccessException.class, () -> {
            userDAO.createUser(user);
        });
    }
    
    @Test
    void getUserPositive() throws Exception {
        UserData user = new UserData("testUser", "password", "test@example.com");
        userDAO.createUser(user);
        
        UserData retrievedUser = userDAO.getUser("testUser");
        Assertions.assertNotNull(retrievedUser);
        Assertions.assertEquals("testUser", retrievedUser.username());
        Assertions.assertEquals("test@example.com", retrievedUser.email());
    }
    
    @Test
    void getUserNegative() throws Exception {
        UserData retrievedUser = userDAO.getUser("nonExistentUser");
        Assertions.assertNull(retrievedUser);
    }
    
    @Test
    void clearPositive() throws Exception {
        UserData user = new UserData("testUser", "password", "test@example.com");
        userDAO.createUser(user);
        
        userDAO.clear();
        
        UserData retrievedUser = userDAO.getUser("testUser");
        Assertions.assertNull(retrievedUser);
    }
}