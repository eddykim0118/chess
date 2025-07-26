package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.UserData;
import model.AuthData;
import java.util.UUID;

public class UserService {
    private final DataAccess dataAccess;

    public UserService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public RegisterResult register(RegisterRequest registerRequest) throws DataAccessException {
        if (registerRequest == null || registerRequest.username() == null ||
                registerRequest.password() == null || registerRequest.email() == null) {
            throw new DataAccessException("Error: bad request");
        }

        if (dataAccess.getUser(registerRequest.username()) != null) {
            throw new DataAccessException("Error: already taken");
        }

        UserData newUser = new UserData(registerRequest.username(), registerRequest.password(), registerRequest.email());
        dataAccess.createUser(newUser);

        String authToken = UUID.randomUUID().toString();
        AuthData auth = new AuthData(authToken, registerRequest.username());
        dataAccess.createAuth(auth);

        return new RegisterResult(registerRequest.username(), authToken);
    }

    public LoginResult login(LoginRequest loginRequest) throws DataAccessException {
        if (loginRequest == null || loginRequest.username() == null || loginRequest.password() == null) {
            throw new DataAccessException("Error: bad request");
        }

        UserData user = dataAccess.getUser(loginRequest.username());
        if (user == null) {
            throw new DataAccessException("Error: unauthorized");
        }

        // Use BCrypt to verify password instead of direct comparison
        if (!org.mindrot.jbcrypt.BCrypt.checkpw(loginRequest.password(), user.getPassword())) {
            throw new DataAccessException("Error: unauthorized");
        }

        String authToken = UUID.randomUUID().toString();
        AuthData auth = new AuthData(authToken, loginRequest.username());
        dataAccess.createAuth(auth);

        return new LoginResult(loginRequest.username(), authToken);
    }

    public void logout(String authToken) throws DataAccessException {
        AuthData auth = dataAccess.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        dataAccess.deleteAuth(authToken);
    }

    public record RegisterRequest(String username, String password, String email) {}
    public record RegisterResult(String username, String authToken) {}
    public record LoginRequest(String username, String password) {}
    public record LoginResult(String username, String authToken) {}
}