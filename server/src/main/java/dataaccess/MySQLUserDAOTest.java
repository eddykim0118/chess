package dataaccess;

import org.junit.jupiter.api.*;
import model.UserData;

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
        // Positive test: User successfully created
        UserData user = new UserData("testUser", "password", "test@example.com");
        userDAO.createUser(user);
        
        UserData retrievedUser = userDAO.getUser("testUser");
        Assertions.assertNotNull(retrievedUser);
        Assertions.assertEquals("testUser", retrievedUser.username());
    }
    
    @Test
    void createUserNegative() throws Exception {
        // Negative test: Attempt to create duplicate user
        UserData user = new UserData("testUser", "password", "test@example.com");
        userDAO.createUser(user);
        
        // Try to create the same user again
        Assertions.assertThrows(DataAccessException.class, () -> {
            userDAO.createUser(user);
        });
    }
    
    @Test
    void getUserPositive() throws Exception {
        // Positive test: Retrieve existing user
        UserData user = new UserData("testUser", "password", "test@example.com");
        userDAO.createUser(user);
        
        UserData retrievedUser = userDAO.getUser("testUser");
        Assertions.assertNotNull(retrievedUser);
        Assertions.assertEquals("testUser", retrievedUser.username());
    }
    
    @Test
    void getUserNegative() throws Exception {
        // Negative test: Attempt to retrieve non-existent user
        UserData retrievedUser = userDAO.getUser("nonExistentUser");
        Assertions.assertNull(retrievedUser);
    }
    
    @Test
    void clearPositive() throws Exception {
        // Positive test: Clear all users
        UserData user = new UserData("testUser", "password", "test@example.com");
        userDAO.createUser(user);
        
        userDAO.clear();
        
        UserData retrievedUser = userDAO.getUser("testUser");
        Assertions.assertNull(retrievedUser);
    }
}