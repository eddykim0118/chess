package service;

import dataAccess.*;
import model.AuthData;
import model.GameData;
import service.requests.CreateGameRequest;
import service.requests.JoinGameRequest;
import service.results.CreateGameResult;
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

    public void joinGame(String authToken, JoinGameRequest request) throws DataAccessException {
        if (authToken == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        
        AuthData auth = authDAO.getAuth(authToken); // Verify the auth token is valid
        
        if (request.gameID() <= 0) {
            throw new DataAccessException("Error: bad request");
        }
        
        GameData game = gameDAO.getGame(request.gameID());
        
        if (request.playerColor() == null) {
            // Just observing the game, no need to update anything
            return;
        }
        
        String color = request.playerColor().toUpperCase();
        if (color.equals("WHITE")) {
            if (game.whiteUsername() != null) {
                throw new DataAccessException("Error: already taken");
            }
            gameDAO.updateGame(request.gameID(), auth.username(), null);
        } else if (color.equals("BLACK")) {
            if (game.blackUsername() != null) {
                throw new DataAccessException("Error: already taken");
            }
            gameDAO.updateGame(request.gameID(), null, auth.username());
        } else {
            throw new DataAccessException("Error: bad request");
        }
    }
}