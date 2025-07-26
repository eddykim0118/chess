package dataaccess;

import model.UserData;
import model.AuthData;
import model.GameData;
import java.util.Collection;

public class MySqlDataAccess implements DataAccess {

    public MySqlDataAccess() throws DataAccessException {
        configureDatabase();
    }

    private void configureDatabase() throws DataAccessException {
        DatabaseManager.createDatabase();
        createTables();
    }

    private void createTables() throws DataAccessException {
        var createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                username VARCHAR(255) NOT NULL PRIMARY KEY,
                password VARCHAR(255) NOT NULL,
                email VARCHAR(255) NOT NULL
            )""";

        var createGamesTable = """
            CREATE TABLE IF NOT EXISTS games (
                gameID INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                whiteUsername VARCHAR(255),
                blackUsername VARCHAR(255),
                gameName VARCHAR(255) NOT NULL,
                gameState TEXT
            )""";

        var createAuthsTable = """
            CREATE TABLE IF NOT EXISTS auths (
                authToken VARCHAR(255) NOT NULL PRIMARY KEY,
                username VARCHAR(255) NOT NULL
            )""";

        try (var conn = DatabaseManager.getConnection()) {
            try (var preparedStatement = conn.prepareStatement(createUsersTable)) {
                preparedStatement.executeUpdate();
            }
            try (var preparedStatement = conn.prepareStatement(createGamesTable)) {
                preparedStatement.executeUpdate();
            }
            try (var preparedStatement = conn.prepareStatement(createAuthsTable)) {
                preparedStatement.executeUpdate();
            }
        } catch (Exception ex) {
            throw new DataAccessException("Unable to configure database", ex);
        }
    }

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