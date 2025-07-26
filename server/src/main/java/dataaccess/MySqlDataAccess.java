package dataaccess;

import model.UserData;
import model.AuthData;
import model.GameData;
import chess.ChessGame;
import org.mindrot.jbcrypt.BCrypt;
import java.util.Collection;
import java.sql.SQLException;
import java.util.ArrayList;
import com.google.gson.Gson;


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
        var clearUsers = "DELETE FROM users";
        var clearGames = "DELETE FROM games";
        var clearAuths = "DELETE FROM auths";

        try (var conn = DatabaseManager.getConnection()) {
            try (var preparedStatement = conn.prepareStatement(clearUsers)) {
                preparedStatement.executeUpdate();
            }
            try (var preparedStatement = conn.prepareStatement(clearGames)) {
                preparedStatement.executeUpdate();
            }
            try (var preparedStatement = conn.prepareStatement(clearAuths)) {
                preparedStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Unable to clear database", ex);
        }
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        if (user == null) {
            throw new DataAccessException("User cannot be null");
        }

        // Hash the password using BCrypt
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());

        var statement = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        try (var conn = DatabaseManager.getConnection()) {
            try (var preparedStatement = conn.prepareStatement(statement)) {
                preparedStatement.setString(1, user.getUsername());
                preparedStatement.setString(2, hashedPassword);
                preparedStatement.setString(3, user.getEmail());
                preparedStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) { // MySQL duplicate entry error code
                throw new DataAccessException("Error: already taken");
            }
            throw new DataAccessException("Unable to create user", ex);
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        if (username == null) {
            throw new DataAccessException("Username cannot be null");
        }

        var statement = "SELECT username, password, email FROM users WHERE username = ?";
        try (var conn = DatabaseManager.getConnection()) {
            try (var preparedStatement = conn.prepareStatement(statement)) {
                preparedStatement.setString(1, username);
                try (var resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        return new UserData(
                                resultSet.getString("username"),
                                resultSet.getString("password"),
                                resultSet.getString("email")
                        );
                    }
                    return null;
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Unable to get user", ex);
        }
    }

    // Keep the other methods stubbed for now
    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        if (auth == null) {
            throw new DataAccessException("Auth cannot be null");
        }

        var statement = "INSERT INTO auths (authToken, username) VALUES (?, ?)";
        try (var conn = DatabaseManager.getConnection()) {
            try (var preparedStatement = conn.prepareStatement(statement)) {
                preparedStatement.setString(1, auth.getAuthToken());
                preparedStatement.setString(2, auth.getUsername());
                preparedStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) { // MySQL duplicate entry error code
                throw new DataAccessException("Error: auth token already exists");
            }
            throw new DataAccessException("Unable to create auth", ex);
        }
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        if (authToken == null) {
            throw new DataAccessException("Auth token cannot be null");
        }

        var statement = "SELECT authToken, username FROM auths WHERE authToken = ?";
        try (var conn = DatabaseManager.getConnection()) {
            try (var preparedStatement = conn.prepareStatement(statement)) {
                preparedStatement.setString(1, authToken);
                try (var resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        return new AuthData(
                                resultSet.getString("authToken"),
                                resultSet.getString("username")
                        );
                    }
                    return null;
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Unable to get auth", ex);
        }
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        if (authToken == null) {
            throw new DataAccessException("Auth token cannot be null");
        }

        var statement = "DELETE FROM auths WHERE authToken = ?";
        try (var conn = DatabaseManager.getConnection()) {
            try (var preparedStatement = conn.prepareStatement(statement)) {
                preparedStatement.setString(1, authToken);
                preparedStatement.executeUpdate();
                // Note: MySQL doesn't throw an error if no rows are deleted
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Unable to delete auth", ex);
        }
    }

    @Override
    public int createGame(GameData game) throws DataAccessException {
        if (game == null) {
            throw new DataAccessException("Game cannot be null");
        }

        // Serialize ChessGame to JSON
        Gson gson = new Gson();
        String gameStateJson = gson.toJson(game.getGame());

        var statement = "INSERT INTO games (whiteUsername, blackUsername, gameName, gameState) VALUES (?, ?, ?, ?)";
        try (var conn = DatabaseManager.getConnection()) {
            try (var preparedStatement = conn.prepareStatement(statement, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setString(1, game.getWhiteUsername());
                preparedStatement.setString(2, game.getBlackUsername());
                preparedStatement.setString(3, game.getGameName());
                preparedStatement.setString(4, gameStateJson);

                preparedStatement.executeUpdate();

                try (var generatedKeys = preparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    } else {
                        throw new DataAccessException("Failed to get generated game ID");
                    }
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Unable to create game", ex);
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        var statement = "SELECT gameID, whiteUsername, blackUsername, gameName, gameState FROM games WHERE gameID = ?";
        try (var conn = DatabaseManager.getConnection()) {
            try (var preparedStatement = conn.prepareStatement(statement)) {
                preparedStatement.setInt(1, gameID);
                try (var resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        // Deserialize ChessGame from JSON
                        Gson gson = new Gson();
                        String gameStateJson = resultSet.getString("gameState");
                        ChessGame chessGame = gson.fromJson(gameStateJson, ChessGame.class);

                        return new GameData(
                                resultSet.getInt("gameID"),
                                resultSet.getString("whiteUsername"),
                                resultSet.getString("blackUsername"),
                                resultSet.getString("gameName"),
                                chessGame
                        );
                    }
                    return null;
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Unable to get game", ex);
        }
    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        var statement = "SELECT gameID, whiteUsername, blackUsername, gameName, gameState FROM games";
        ArrayList<GameData> games = new ArrayList<>();

        try (var conn = DatabaseManager.getConnection()) {
            try (var preparedStatement = conn.prepareStatement(statement)) {
                try (var resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        // Deserialize ChessGame from JSON
                        Gson gson = new Gson();
                        String gameStateJson = resultSet.getString("gameState");
                        ChessGame chessGame = gson.fromJson(gameStateJson, ChessGame.class);

                        GameData gameData = new GameData(
                                resultSet.getInt("gameID"),
                                resultSet.getString("whiteUsername"),
                                resultSet.getString("blackUsername"),
                                resultSet.getString("gameName"),
                                chessGame
                        );
                        games.add(gameData);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Unable to list games", ex);
        }

        return games;
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        if (game == null) {
            throw new DataAccessException("Game cannot be null");
        }

        // Serialize ChessGame to JSON
        Gson gson = new Gson();
        String gameStateJson = gson.toJson(game.getGame());

        var statement = "UPDATE games SET whiteUsername = ?, blackUsername = ?, gameName = ?, gameState = ? WHERE gameID = ?";
        try (var conn = DatabaseManager.getConnection()) {
            try (var preparedStatement = conn.prepareStatement(statement)) {
                preparedStatement.setString(1, game.getWhiteUsername());
                preparedStatement.setString(2, game.getBlackUsername());
                preparedStatement.setString(3, game.getGameName());
                preparedStatement.setString(4, gameStateJson);
                preparedStatement.setInt(5, game.getGameID());

                int rowsAffected = preparedStatement.executeUpdate();
                if (rowsAffected == 0) {
                    throw new DataAccessException("Game not found");
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Unable to update game", ex);
        }
    }
}