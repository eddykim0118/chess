package dataaccess;

import model.UserData;
import model.AuthData;

public interface DataAccess {

    void clear() throws DataAccessException;

    void createUser(UserData user) throws DataAccessException;
    UserData getUser(String username) throws DataAccessException;

    void createAuth(AuthData auth) throws DataAccessException;
    AuthData getAuth(String authToken) throws DataAccessException;
    void deleteAuth(String authToken) throws DataAccessException;
}

