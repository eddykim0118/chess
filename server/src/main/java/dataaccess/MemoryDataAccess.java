package dataaccess;

import model.UserData;
import model.GameData;
import model.AuthData;

import java.util.HashMap;
import java.util.Map;

public class MemoryDataAccess implements DataAccess {

    // In-memory storage using Maps
    private final Map<String, UserData> users = new HashMap<>();
    private final Map<String, AuthData> authTokens = new HashMap<>();
//    private final Map<Integer, GameData> games = new HashMap<>();

    @Override
    public void clear() throws DataAccessException {
        users.clear();
        authTokens.clear();
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        if (user == null) {
            throw new DataAccessException("User cannot be null");
        }
        if (users.containsKey(user.username())) {
            throw new DataAccessException("User already exists");
        }
        users.put(user.username(), user);
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        if (username == null) {
            throw new DataAccessException("Username cannot be null");
        }
        return users.get(username);
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        if (auth == null) {
            throw new DataAccessException("Auth cannot be null");
        }
        authTokens.put(auth.authToken(), auth);
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        if (authToken == null) {
            throw new DataAccessException("Auth token cannot be null");
        }
        return authTokens.get(authToken);
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        if (authToken == null) {
            throw new DataAccessException("Auth token cannot be null");
        }
        authTokens.remove(authToken);
    }
}