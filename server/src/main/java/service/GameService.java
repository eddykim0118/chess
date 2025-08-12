package service;

import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import java.util.Collection;

public class GameService {
    private final DataAccess dataAccess;

    public GameService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public ListGamesResult listGames(String authToken) throws DataAccessException {
        AuthData auth = dataAccess.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("Error: unauthorized");
        }

        Collection<GameData> games = dataAccess.listGames();
        return new ListGamesResult(games);
    }

    public CreateGameResult createGame(CreateGameRequest request, String authToken) throws DataAccessException {
        AuthData auth = dataAccess.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("Error: unauthorized");
        }

        if (request == null || request.gameName() == null) {
            throw new DataAccessException("Error: bad request");
        }

        ChessGame game = new ChessGame();
        GameData gameData = new GameData(0, null, null, request.gameName(), game);
        int gameID = dataAccess.createGame(gameData);

        return new CreateGameResult(gameID);
    }

    public void joinGame(JoinGameRequest request, String authToken) throws DataAccessException {
        AuthData auth = dataAccess.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("Error: unauthorized");
        }

        if (request == null || request.gameID() == 0) {
            throw new DataAccessException("Error: bad request");
        }

        GameData game = dataAccess.getGame(request.gameID());
        if (game == null) {
            throw new DataAccessException("Error: bad request");
        }

        String username = auth.getUsername();
        GameData updatedGame;

        // Validate playerColor first - must be "WHITE", "BLACK", null, or empty
        if (request.playerColor() != null && !request.playerColor().isEmpty()) {
            if (!"WHITE".equals(request.playerColor()) && !"BLACK".equals(request.playerColor())) {
                // Invalid color like "PURPLE", "invalid", "white", "black", etc.
                throw new DataAccessException("Error: bad request");
            }
        }

        if ("WHITE".equals(request.playerColor())) {
            if (game.getWhiteUsername() != null) {
                throw new DataAccessException("Error: already taken");
            }
            updatedGame = new GameData(game.getGameID(), username, game.getBlackUsername(), game.getGameName(), game.getGame());
            dataAccess.updateGame(updatedGame);
        } else if ("BLACK".equals(request.playerColor())) {
            if (game.getBlackUsername() != null) {
                throw new DataAccessException("Error: already taken");
            }
            updatedGame = new GameData(game.getGameID(), game.getWhiteUsername(), username, game.getGameName(), game.getGame());
            dataAccess.updateGame(updatedGame);
        }
        // Observer case (null or empty) - no database update needed
    }


    public record ListGamesResult(Collection<GameData> games) {}
    public record CreateGameRequest(String gameName) {}
    public record CreateGameResult(int gameID) {}
    public record JoinGameRequest(String playerColor, int gameID) {}
}