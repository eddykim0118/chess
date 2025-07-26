package dataaccess;

import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserDAOTests {
    private MySqlDataAccess dataAccess;

    @BeforeEach
    public void setUp() throws DataAccessException {
        dataAccess = new MySqlDataAccess();
        dataAccess.clear(); // Clear data before each test
    }

    @Test
    public void createUserPositive() throws DataAccessException {
        UserData user = new UserData("testuser", "password123", "test@example.com");
        dataAccess.createUser(user);

        UserData retrieved = dataAccess.getUser("testuser");
        assertNotNull(retrieved);
        assertEquals("testuser", retrieved.getUsername());
        assertEquals("test@example.com", retrieved.getEmail());
        // Password should be hashed, so it shouldn't match the original
        assertNotEquals("password123", retrieved.getPassword());
    }

    @Test
    public void createUserNegative() throws DataAccessException {
        UserData user = new UserData("testuser", "password123", "test@example.com");
        dataAccess.createUser(user);

        // Try to create the same user again - should throw exception
        assertThrows(DataAccessException.class, () -> {
            dataAccess.createUser(user);
        });
    }

    @Test
    public void getUserPositive() throws DataAccessException {
        UserData user = new UserData("testuser", "password123", "test@example.com");
        dataAccess.createUser(user);

        UserData retrieved = dataAccess.getUser("testuser");
        assertNotNull(retrieved);
        assertEquals("testuser", retrieved.getUsername());
    }

    @Test
    public void getUserNegative() throws DataAccessException {
        UserData retrieved = dataAccess.getUser("nonexistent");
        assertNull(retrieved);
    }

    @Test
    public void clearPositive() throws DataAccessException {
        UserData user = new UserData("testuser", "password123", "test@example.com");
        dataAccess.createUser(user);

        dataAccess.clear();

        UserData retrieved = dataAccess.getUser("testuser");
        assertNull(retrieved);
    }
}