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

    public RegisterResult register(RegisterRequest registerRequest) throws ServiceException {
        try {
            if (registerRequest == null) {
                throw new ServiceException("Bad request");
            }

            String username = registerRequest.username();
            String password = registerRequest.password();
            String email = registerRequest.email();

            if (username == null || username.trim().isEmpty() ||
                    password == null || password.trim().isEmpty() ||
                    email == null || email.trim().isEmpty()) {
                throw new ServiceException("Bad request");
            }

            UserData existingUser = dataAccess.getUser(username);
            if (existingUser != null) {
                throw new ServiceException("Already taken");
            }

            UserData newUser = new UserData(username, password, email);
            dataAccess.createUser(newUser);

            String authToken = UUID.randomUUID().toString();
            AuthData auth = new AuthData(authToken, username);
            dataAccess.createAuth(auth);

            return new RegisterResult(username, authToken);
        } catch (DataAccessException e) {
            throw new ServiceException("Internal server error: " + e.getMessage());
        }
    }

    public record RegisterRequest(String username, String password, String email) {}
    public record RegisterResult(String username, String authToken) {}
}
