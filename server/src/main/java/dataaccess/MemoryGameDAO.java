package dataAccess;

import chess.ChessGame;
import model.GameData;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MemoryGameDAO implements GameDAO {
    private final Map<Integer, GameData> games = new HashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    @Override
    public void clear() {
        games.clear();
        nextId.set(1);
    }

    @Override
    public int createGame(String gameName) throws DataAccessException {
        if (gameName == null) {
            throw new DataAccessException("Error: bad request");
        }
        
        int gameID = nextId.getAndIncrement();
        games.put(gameID, new GameData(gameID, null, null, gameName, new ChessGame()));
        return gameID;
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        GameData game = games.get(gameID);
        if (game == null) {
            throw new DataAccessException("Error: bad request");
        }
        return game;
    }

    @Override
    public Collection<GameData> listGames() {
        return games.values();
    }

    @Override
    public void updateGame(int gameID, String whiteUsername, String blackUsername) throws DataAccessException {
        GameData game = getGame(gameID);
        
        String newWhite = whiteUsername != null ? whiteUsername : game.whiteUsername();
        String newBlack = blackUsername != null ? blackUsername : game.blackUsername();
        
        games.put(gameID, new GameData(gameID, newWhite, newBlack, game.gameName(), game.game()));
    }
}