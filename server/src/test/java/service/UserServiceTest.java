package service;

import dataaccess.*;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.requests.LoginRequest;
import service.requests.LogoutRequest;
import service.requests.RegisterRequest;
import service.results.LoginResult;
import service.results.RegisterResult;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {
    private UserService userService;
    private UserDAO userDAO;
    private AuthDAO authDAO;

    @BeforeEach
    public void setUp() {
        userDAO = new MemoryUserDAO();
        authDAO = new MemoryAuthDAO();
        userService = new UserService(userDAO, authDAO);
    }

    @Test
    public void registerPositive() throws DataAccessException {
        // Test successful registration
        RegisterRequest request = new RegisterRequest("user1", "password", "user1@example.com");
        RegisterResult result = userService.register(request);
        
        assertNotNull(result);
        assertEquals("user1", result.username());
        assertNotNull(result.authToken());
    }

    @Test
    public void registerNegative() {
        // Test registration with missing data
        RegisterRequest badRequest = new RegisterRequest(null, "password", "email");
        assertThrows(DataAccessException.class, () -> userService.register(badRequest));
    }

    @Test
    public void loginPositive() throws DataAccessException {
        // Register a user first
        userDAO.createUser(new UserData("user3", "password", "user3@example.com"));
        
        // Test successful login
        LoginRequest request = new LoginRequest("user3", "password");
        LoginResult result = userService.login(request);
        
        assertNotNull(result);
        assertEquals("user3", result.username());
        assertNotNull(result.authToken());
    }

    @Test
    public void loginNegative() throws DataAccessException {
        // Register a user first
        userDAO.createUser(new UserData("user4", "password", "user4@example.com"));
        
        // Test login with wrong password
        LoginRequest badPasswordRequest = new LoginRequest("user4", "wrongpassword");
        assertThrows(DataAccessException.class, () -> userService.login(badPasswordRequest));
    }

    @Test
    public void logoutPositive() throws DataAccessException {
        // Register user and get auth token
        String authToken = authDAO.createAuth("user5");
        
        // Test successful logout
        LogoutRequest request = new LogoutRequest(authToken);
        assertDoesNotThrow(() -> userService.logout(request));
    }

    @Test
    public void logoutNegative() {
        // Test logout with invalid auth token
        LogoutRequest request = new LogoutRequest("invalidauthtoken");
        assertThrows(DataAccessException.class, () -> userService.logout(request));
    }
}