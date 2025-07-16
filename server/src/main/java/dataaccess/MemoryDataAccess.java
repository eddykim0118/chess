package dataaccess;

import model.UserData;
import model.GameData;
import model.AuthData;

import java.util.Map;
import java.util.HashMap;

public class MemoryDataAccess implements DataAcess {
    private final Map<String, UserData> users = new HashMap<>();
    private final Map<String, AuthData> authTokens = new HashMap<>();
    private final Map<String, GameData> games = new HashMap<>();

    private int nextGameID = 1;

    @Override
    public void clear() throws DataAccessException {
        users.clear();
        authTokens.clear();
        games.clear();
        nextGameID = 1;
    }
}
