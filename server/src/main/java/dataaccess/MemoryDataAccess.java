package dataaccess;

import model.UserData;
import model.GameData;
import model.AuthData;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory implementation of DataAccess interface
 * Stores all data in memory using Maps and other data structures
 */
public class MemoryDataAccess implements DataAccess {

    // In-memory storage using Maps
    private final Map<String, UserData> users = new HashMap<>();
    private final Map<String, AuthData> authTokens = new HashMap<>();
    private final Map<Integer, GameData> games = new HashMap<>();

    // For generating unique game IDs
    private int nextGameID = 1;

    @Override
    public void clear() throws DataAccessException {
        users.clear();
        authTokens.clear();
        games.clear();
        nextGameID = 1;
    }

}