package dataaccess;

import model.AuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AuthDAOTests {
    private MySqlDataAccess dataAccess;

    @BeforeEach
    public void setUp() throws DataAccessException {
        dataAccess = new MySqlDataAccess();
        dataAccess.clear(); // Clear data before each test
    }

    @Test
    public void createAuthPositive() throws DataAccessException {
        AuthData auth = new AuthData("token123", "testuser");
        dataAccess.createAuth(auth);

        AuthData retrieved = dataAccess.getAuth("token123");
        assertNotNull(retrieved);
        assertEquals("token123", retrieved.getAuthToken());
        assertEquals("testuser", retrieved.getUsername());
    }

    @Test
    public void createAuthNegative() throws DataAccessException {
        AuthData auth = new AuthData("token123", "testuser");
        dataAccess.createAuth(auth);

        // Try to create auth with same token - should throw exception
        AuthData duplicate = new AuthData("token123", "otheruser");
        assertThrows(DataAccessException.class, () -> {
            dataAccess.createAuth(duplicate);
        });
    }

    @Test
    public void getAuthPositive() throws DataAccessException {
        AuthData auth = new AuthData("token123", "testuser");
        dataAccess.createAuth(auth);

        AuthData retrieved = dataAccess.getAuth("token123");
        assertNotNull(retrieved);
        assertEquals("testuser", retrieved.getUsername());
    }

    @Test
    public void getAuthNegative() throws DataAccessException {
        AuthData retrieved = dataAccess.getAuth("nonexistent");
        assertNull(retrieved);
    }

    @Test
    public void deleteAuthPositive() throws DataAccessException {
        AuthData auth = new AuthData("token123", "testuser");
        dataAccess.createAuth(auth);

        dataAccess.deleteAuth("token123");

        AuthData retrieved = dataAccess.getAuth("token123");
        assertNull(retrieved);
    }

    @Test
    public void deleteAuthNegative() throws DataAccessException {
        // Deleting non-existent auth should not throw exception
        assertDoesNotThrow(() -> {
            dataAccess.deleteAuth("nonexistent");
        });
    }

    @Test
    public void clearPositive() throws DataAccessException {
        AuthData auth = new AuthData("token123", "testuser");
        dataAccess.createAuth(auth);

        dataAccess.clear();

        AuthData retrieved = dataAccess.getAuth("token123");
        assertNull(retrieved);
    }
}