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

        // Debug logging
        System.out.println("PlayerColor received: '" + request.playerColor() + "' (length: " + 
                           (request.playerColor() == null ? "null" : request.playerColor().length()) + ")");

        // Validate playerColor - must be exactly "WHITE" or "BLACK" 
        // Null is NOT allowed (observers must be handled differently)
        if (request.playerColor() == null) {
            System.out.println("Throwing bad request exception for null playerColor");
            throw new DataAccessException("Error: bad request");
        }
        
        if (!"WHITE".equals(request.playerColor()) && !"BLACK".equals(request.playerColor())) {
            System.out.println("Throwing bad request exception for invalid playerColor: '" + request.playerColor() + "'");
            throw new DataAccessException("Error: bad request");
        }

        // Now we know playerColor is either "WHITE" or "BLACK"
        if ("WHITE".equals(request.playerColor())) {
            if (game.getWhiteUsername() != null) {
                throw new DataAccessException("Error: already taken");
            }
            updatedGame = new GameData(game.getGameID(), username, game.getBlackUsername(), game.getGameName(), game.getGame());
            dataAccess.updateGame(updatedGame);
        } else { // Must be "BLACK"
            if (game.getBlackUsername() != null) {
                throw new DataAccessException("Error: already taken");
            }
            updatedGame = new GameData(game.getGameID(), game.getWhiteUsername(), username, game.getGameName(), game.getGame());
            dataAccess.updateGame(updatedGame);
        }
    }

    public record ListGamesResult(Collection<GameData> games) {}
    public record CreateGameRequest(String gameName) {}
    public record CreateGameResult(int gameID) {}
    public record JoinGameRequest(String playerColor, int gameID) {}
}