package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.UserData;
import model.AuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    private DataAccess dataAccess;
    private UserService userService;

    @BeforeEach
    public void setUp() {
        dataAccess = new MemoryDataAccess();
        userService = new UserService(dataAccess);
    }

    @Test
    @DisplayName("Register Success")
    public void registerSuccess() throws Exception {
        UserService.RegisterRequest request = new UserService.RegisterRequest("testuser", "password", "test@email.com");
        UserService.RegisterResult result = userService.register(request);

        assertNotNull(result);
        assertEquals("testuser", result.username());
        assertNotNull(result.authToken());

        UserData createdUser = dataAccess.getUser("testuser");
        assertNotNull(createdUser);
        assertEquals("testuser", createdUser.getUsername());

        AuthData createdAuth = dataAccess.getAuth(result.authToken());
        assertNotNull(createdAuth);
        assertEquals("testuser", createdAuth.getUsername());
    }

    @Test
    @DisplayName("Register Failure - Duplicate Username")
    public void registerFailureDuplicate() throws Exception {
        UserService.RegisterRequest request1 = new UserService.RegisterRequest("testuser", "password", "test@email.com");
        userService.register(request1);

        UserService.RegisterRequest request2 = new UserService.RegisterRequest("testuser", "different", "different@email.com");

        DataAccessException exception = assertThrows(DataAccessException.class, () -> {
            userService.register(request2);
        });

        assertTrue(exception.getMessage().contains("already taken"));
    }

    @Test
    @DisplayName("Register Failure - Null Username")
    public void registerFailureNullUsername() {
        UserService.RegisterRequest request = new UserService.RegisterRequest(null, "password", "test@email.com");

        DataAccessException exception = assertThrows(DataAccessException.class, () -> {
            userService.register(request);
        });

        assertTrue(exception.getMessage().contains("bad request"));
    }

    @Test
    @DisplayName("Register Failure - Null Password")
    public void registerFailureNullPassword() {
        UserService.RegisterRequest request = new UserService.RegisterRequest("testuser", null, "test@email.com");

        DataAccessException exception = assertThrows(DataAccessException.class, () -> {
            userService.register(request);
        });

        assertTrue(exception.getMessage().contains("bad request"));
    }

    @Test
    @DisplayName("Login Success")
    public void loginSuccess() throws Exception {
        UserService.RegisterRequest registerRequest = new UserService.RegisterRequest("testuser", "password", "test@email.com");
        userService.register(registerRequest);

        UserService.LoginRequest loginRequest = new UserService.LoginRequest("testuser", "password");
        UserService.LoginResult result = userService.login(loginRequest);

        assertNotNull(result);
        assertEquals("testuser", result.username());
        assertNotNull(result.authToken());
    }

    @Test
    @DisplayName("Login Failure - Wrong Password")
    public void loginFailureWrongPassword() throws Exception {
        UserService.RegisterRequest registerRequest = new UserService.RegisterRequest("testuser", "password", "test@email.com");
        userService.register(registerRequest);

        UserService.LoginRequest loginRequest = new UserService.LoginRequest("testuser", "wrongpassword");

        DataAccessException exception = assertThrows(DataAccessException.class, () -> {
            userService.login(loginRequest);
        });

        assertTrue(exception.getMessage().contains("unauthorized"));
    }

    @Test
    @DisplayName("Login Failure - User Not Found")
    public void loginFailureUserNotFound() {
        UserService.LoginRequest loginRequest = new UserService.LoginRequest("nonexistent", "password");

        DataAccessException exception = assertThrows(DataAccessException.class, () -> {
            userService.login(loginRequest);
        });

        assertTrue(exception.getMessage().contains("unauthorized"));
    }

    @Test
    @DisplayName("Logout Success")
    public void logoutSuccess() throws Exception {
        UserService.RegisterRequest registerRequest = new UserService.RegisterRequest("testuser", "password", "test@email.com");
        UserService.RegisterResult registerResult = userService.register(registerRequest);

        assertDoesNotThrow(() -> {
            userService.logout(registerResult.authToken());
        });

        AuthData auth = dataAccess.getAuth(registerResult.authToken());
        assertNull(auth);
    }

    @Test
    @DisplayName("Logout Failure - Invalid Token")
    public void logoutFailureInvalidToken() {
        DataAccessException exception = assertThrows(DataAccessException.class, () -> {
            userService.logout("invalidtoken");
        });

        assertTrue(exception.getMessage().contains("unauthorized"));
    }
}