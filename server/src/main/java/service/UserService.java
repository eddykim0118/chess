package service;

import dataAccess.*;
import model.UserData;
import service.requests.LoginRequest;
import service.requests.LogoutRequest;
import service.requests.RegisterRequest;
import service.results.LoginResult;
import service.results.RegisterResult;

public class UserService {
    private final UserDAO userDAO;
    private final AuthDAO authDAO;

    public UserService(UserDAO userDAO, AuthDAO authDAO) {
        this.userDAO = userDAO;
        this.authDAO = authDAO;
    }

    public RegisterResult register(RegisterRequest request) throws DataAccessException {
        if (request.username() == null || request.password() == null || request.email() == null) {
            throw new DataAccessException("Error: bad request");
        }

        UserData user = new UserData(request.username(), request.password(), request.email());
        userDAO.createUser(user);
        
        String authToken = authDAO.createAuth(request.username());
        return new RegisterResult(request.username(), authToken);
    }

    public LoginResult login(LoginRequest request) throws DataAccessException {
        if (request.username() == null || request.password() == null) {
            throw new DataAccessException("Error: bad request");
        }

        UserData user = userDAO.getUser(request.username());
        
        if (!user.password().equals(request.password())) {
            throw new DataAccessException("Error: unauthorized");
        }
        
        String authToken = authDAO.createAuth(request.username());
        return new LoginResult(request.username(), authToken);
    }

    public void logout(LogoutRequest request) throws DataAccessException {
        if (request.authToken() == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        
        authDAO.deleteAuth(request.authToken());
    }
}