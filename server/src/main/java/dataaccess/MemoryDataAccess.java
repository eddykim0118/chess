package dataaccess;

import model.UserData;
import model.GameData;
import model.AuthData;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

public class MemoryDataAccess implements DataAccess {
    private final Map<String, UserData> users = new HashMap<>();
    private final Map<String, AuthData> authTokens = new HashMap<>();
    private final Map<Integer, GameData> games = new HashMap<>();
    private int nextGameID = 1;

    @Override
    public void clear() throws DataAccessException {
        users.clear();
        authTokens.clear();
        games.clear();
        nextGameID = 1;
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        if (user == null) {
            throw new DataAccessException("User cannot be null");
        }
        if (users.containsKey(user.getUsername())) {
            throw new DataAccessException("User already exists");
        }
        users.put(user.getUsername(), user);
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
        authTokens.put(auth.getAuthToken(), auth);
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

    @Override
    public int createGame(GameData game) throws DataAccessException {
        if (game == null) {
            throw new DataAccessException("Game cannot be null");
        }
        int gameID = nextGameID++;
        GameData newGame = new GameData(gameID, game.getWhiteUsername(), game.getBlackUsername(), game.getGameName(), game.getGame());
        games.put(gameID, newGame);
        return gameID;
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        return games.get(gameID);
    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        return games.values();
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        if (game == null) {
            throw new DataAccessException("Game cannot be null");
        }
        games.put(game.getGameID(), game);
    }
}