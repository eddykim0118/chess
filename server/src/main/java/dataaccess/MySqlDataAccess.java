package dataaccess;

import model.UserData;
import model.AuthData;
import model.GameData;
import java.util.Collection;

public class MySqlDataAccess implements DataAccess {

    @Override
    public void clear() throws DataAccessException {
        // TODO: Implement MySQL clear
        throw new DataAccessException("Method not implemented yet");
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        // TODO: Implement MySQL createUser
        throw new DataAccessException("Method not implemented yet");
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        // TODO: Implement MySQL getUser
        throw new DataAccessException("Method not implemented yet");
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        // TODO: Implement MySQL createAuth
        throw new DataAccessException("Method not implemented yet");
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        // TODO: Implement MySQL getAuth
        throw new DataAccessException("Method not implemented yet");
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        // TODO: Implement MySQL deleteAuth
        throw new DataAccessException("Method not implemented yet");
    }

    @Override
    public int createGame(GameData game) throws DataAccessException {
        // TODO: Implement MySQL createGame
        throw new DataAccessException("Method not implemented yet");
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        // TODO: Implement MySQL getGame
        throw new DataAccessException("Method not implemented yet");
    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        // TODO: Implement MySQL listGames
        throw new DataAccessException("Method not implemented yet");
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        // TODO: Implement MySQL updateGame
        throw new DataAccessException("Method not implemented yet");
    }
}