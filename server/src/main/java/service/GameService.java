package service;

import chess.ChessGame;
import dataaccess.*;
import model.AuthData;
import model.GameData;
import service.requests.CreateGameRequest;
import service.requests.JoinGameRequest;
import service.results.CreateGameResult;
import service.results.JoinGameResult;
import service.results.ListGamesResult;
import java.util.Collection;

public class GameService {
    private final GameDAO gameDAO;
    private final AuthDAO authDAO;

    public GameService(GameDAO gameDAO, AuthDAO authDAO) {
        this.gameDAO = gameDAO;
        this.authDAO = authDAO;
    }

    public ListGamesResult listGames(String authToken) throws DataAccessException {
        if (authToken == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        
        authDAO.getAuth(authToken); // Verify the auth token is valid
        Collection<GameData> games = gameDAO.listGames();
        return new ListGamesResult(games);
    }

    public CreateGameResult createGame(String authToken, CreateGameRequest request) throws DataAccessException {
        if (authToken == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        
        authDAO.getAuth(authToken); // Verify the auth token is valid
        
        if (request.gameName() == null) {
            throw new DataAccessException("Error: bad request");
        }
        
        int gameID = gameDAO.createGame(request.gameName());
        return new CreateGameResult(gameID);
    }

    public JoinGameResult joinGame(String authToken, JoinGameRequest request) throws DataAccessException {
        if (authToken == null || request == null || request.gameID() <= 0) {
            throw new DataAccessException("Error: bad request");
        }
        
        // Add this validation for team color
        String playerColor = request.playerColor();
        if (playerColor != null) {
            try {
                ChessGame.TeamColor.valueOf(playerColor);
            } catch (IllegalArgumentException e) {
                throw new DataAccessException("Error: bad request");
            }
        } else if (playerColor == null || playerColor.isEmpty()) {
            throw new DataAccessException("Error: bad request");
        }
        
        // Rest of your existing code...
        // Check authorization
        AuthData auth = authDAO.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        
        // Get the game
        GameData game = gameDAO.getGame(request.gameID());
        if (game == null) {
            throw new DataAccessException("Error: bad request");
        }
        
        // Check if the color is already taken
        if (playerColor.equals("WHITE") && game.whiteUsername() != null) {
            throw new DataAccessException("Error: already taken");
        } else if (playerColor.equals("BLACK") && game.blackUsername() != null) {
            throw new DataAccessException("Error: already taken");
        }
        
        // Update the game with the player
        gameDAO.updateGame(request.gameID(), 
                          playerColor.equals("WHITE") ? auth.username() : game.whiteUsername(),
                          playerColor.equals("BLACK") ? auth.username() : game.blackUsername());
        
        return new JoinGameResult();
    }
}