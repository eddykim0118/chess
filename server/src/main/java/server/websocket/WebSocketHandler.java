package server.websocket;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import chess.InvalidMoveException;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MySqlDataAccess;
import model.GameData;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@WebSocket
public class WebSocketHandler {

    private final Gson gson = new Gson();
    private static DataAccess dataAccess;

    private static final ConcurrentHashMap<Integer, CopyOnWriteArraySet<Session>> GAME_SESSIONS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Session, SessionInfo> SESSION_INFO = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Boolean> RESIGNED_GAMES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Session, String> SESSION_MESSAGES = new ConcurrentHashMap<>();

    public static void setDataAccess(DataAccess da) {
        dataAccess = da;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) throws Exception {
        System.out.println("WebSocket connection established");
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("WebSocket connection closed");
        SessionInfo info = SESSION_INFO.remove(session);
        SESSION_MESSAGES.remove(session);
        if (info != null) {
            CopyOnWriteArraySet<Session> sessions = GAME_SESSIONS.get(info.gameID());
            if (sessions != null) {
                sessions.remove(session);
            }
        }
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws Exception {
        SESSION_MESSAGES.put(session, message);

        UserGameCommand command = gson.fromJson(message, UserGameCommand.class);

        switch (command.getCommandType()) {
            case CONNECT -> handleConnect(session, command);
            case MAKE_MOVE -> handleMakeMove(session, command);
            case LEAVE -> handleLeave(session, command);
            case RESIGN -> handleResign(session, command);
        }
    }

    private void handleConnect(Session session, UserGameCommand command) {
        try {
            if (command.getAuthToken() == null || command.getAuthToken().trim().isEmpty()) {
                sendError(session, "Error: Invalid auth token");
                return;
            }

            if (command.getGameID() == null) {
                sendError(session, "Error: Invalid game ID");
                return;
            }

            var authData = dataAccess.getAuth(command.getAuthToken());
            if (authData == null) {
                sendError(session, "Error: Invalid auth token");
                return;
            }
            String username = authData.getUsername();

            GameData gameData = dataAccess.getGame(command.getGameID());
            if (gameData == null) {
                sendError(session, "Error: Game not found");
                return;
            }

            GAME_SESSIONS.computeIfAbsent(command.getGameID(), k -> new CopyOnWriteArraySet<>()).add(session);

            String role;
            if (username.equals(gameData.getWhiteUsername())) {
                role = username + " joined as white player";
            } else if (username.equals(gameData.getBlackUsername())) {
                role = username + " joined as black player";
            } else {
                role = username + " joined as observer";
            }

            SESSION_INFO.put(session, new SessionInfo(username, command.getGameID(), role));

            LoadGameMessage loadMessage = new LoadGameMessage(gameData.getGame());
            sendMessage(session, loadMessage);
            NotificationMessage notification = new NotificationMessage(role);
            broadcastToOthers(command.getGameID(), session, notification);

        } catch (DataAccessException e) {
            sendError(session, "Error: " + e.getMessage());
        } catch (Exception e) {
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private void handleMakeMove(Session session, UserGameCommand command) {
        try {
            if (RESIGNED_GAMES.getOrDefault(command.getGameID(), false)) {
                sendError(session, "Error: Game is over due to resignation");
                return;
            }

            if (!validateCommand(session, command)) return;

            String username = validateAndGetUsername(session, command);
            if (username == null) return;

            GameData gameData = validateAndGetGame(session, command);
            if (gameData == null) return;

            ChessGame game = gameData.getGame();

            if (isGameOver(command.getGameID(), game)) {
                sendError(session, "Error: Game is over");
                return;
            }

            ChessMove move = extractMoveFromMessage(session);
            if (move == null) return;

            ChessGame.TeamColor playerColor = validatePlayerTurn(session, username, gameData, game);
            if (playerColor == null) return;

            executeMoveAndUpdate(session, command, game, gameData, move, username, playerColor);

        } catch (DataAccessException e) {
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private void handleLeave(Session session, UserGameCommand command) {
        try {
            if (command.getAuthToken() == null || command.getAuthToken().trim().isEmpty()) {
                sendError(session, "Error: Invalid auth token");
                return;
            }
            if (command.getGameID() == null) {
                sendError(session, "Error: Invalid game ID");
                return;
            }

            var authData = dataAccess.getAuth(command.getAuthToken());
            if (authData == null) {
                sendError(session, "Error: Invalid auth token");
                return;
            }
            String username = authData.getUsername();

            GameData gameData = dataAccess.getGame(command.getGameID());
            if (gameData == null) {
                sendError(session, "Error: Game not found");
                return;
            }

            boolean isPlayer = username.equals(gameData.getWhiteUsername()) ||
                    username.equals(gameData.getBlackUsername());

            if (isPlayer) {
                String newWhiteUsername = username.equals(gameData.getWhiteUsername()) ? null : gameData.getWhiteUsername();
                String newBlackUsername = username.equals(gameData.getBlackUsername()) ? null : gameData.getBlackUsername();

                GameData updatedGameData = new GameData(gameData.getGameID(), newWhiteUsername,
                        newBlackUsername, gameData.getGameName(), gameData.getGame());
                dataAccess.updateGame(updatedGameData);
            }

            CopyOnWriteArraySet<Session> sessions = GAME_SESSIONS.get(command.getGameID());
            if (sessions != null) {
                sessions.remove(session);
            }
            SESSION_INFO.remove(session);
            SESSION_MESSAGES.remove(session);

            NotificationMessage notification = new NotificationMessage(username + " left the game");
            broadcastToOthers(command.getGameID(), session, notification);

        } catch (DataAccessException e) {
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private void handleResign(Session session, UserGameCommand command) {
        try {
            if (command.getAuthToken() == null || command.getAuthToken().trim().isEmpty()) {
                sendError(session, "Error: Invalid auth token");
                return;
            }
            if (command.getGameID() == null) {
                sendError(session, "Error: Invalid game ID");
                return;
            }

            var authData = dataAccess.getAuth(command.getAuthToken());
            if (authData == null) {
                sendError(session, "Error: Invalid auth token");
                return;
            }
            String username = authData.getUsername();

            GameData gameData = dataAccess.getGame(command.getGameID());
            if (gameData == null) {
                sendError(session, "Error: Game not found");
                return;
            }

            ChessGame game = gameData.getGame();

            if (isGameOver(command.getGameID(), game)) {
                sendError(session, "Error: Game is already over");
                return;
            }

            boolean isWhitePlayer = username.equals(gameData.getWhiteUsername());
            boolean isBlackPlayer = username.equals(gameData.getBlackUsername());

            if (!isWhitePlayer && !isBlackPlayer) {
                sendError(session, "Error: Observer cannot resign");
                return;
            }

            RESIGNED_GAMES.put(command.getGameID(), true);

            NotificationMessage resignNotification = new NotificationMessage(username + " resigned. Game is over.");
            broadcastToAll(command.getGameID(), resignNotification);

        } catch (DataAccessException e) {
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private boolean isGameOver(Integer gameID, ChessGame game) {
        if (RESIGNED_GAMES.getOrDefault(gameID, false)) {
            return true;
        }

        return game.isInCheckmate(ChessGame.TeamColor.WHITE) ||
                game.isInCheckmate(ChessGame.TeamColor.BLACK) ||
                game.isInStalemate(ChessGame.TeamColor.WHITE) ||
                game.isInStalemate(ChessGame.TeamColor.BLACK);
    }

    private void broadcastToAll(Integer gameID, Object message) {
        CopyOnWriteArraySet<Session> sessions = GAME_SESSIONS.get(gameID);
        if (sessions != null) {
            for (Session session : sessions) {
                sendMessage(session, message);
            }
        }
    }

    private String formatMove(ChessMove move, ChessGame.TeamColor playerColor) {
        String startPos = positionToString(move.getStartPosition());
        String endPos = positionToString(move.getEndPosition());
        String result = startPos + " to " + endPos;

        if (move.getPromotionPiece() != null) {
            result += " (promoted to " + move.getPromotionPiece() + ")";
        }

        return result;
    }

    private String positionToString(ChessPosition position) {
        char col = (char) ('a' + position.getColumn() - 1);
        return "" + col + position.getRow();
    }

    private void sendMessage(Session session, Object message) {
        try {
            session.getRemote().sendString(gson.toJson(message));
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    private void sendError(Session session, String errorMessage) {
        sendMessage(session, new ErrorMessage(errorMessage));
    }

    private void broadcastToOthers(Integer gameID, Session excludeSession, Object message) {
        CopyOnWriteArraySet<Session> sessions = GAME_SESSIONS.get(gameID);
        if (sessions != null) {
            for (Session session : sessions) {
                if (!session.equals(excludeSession)) {
                    sendMessage(session, message);
                }
            }
        }
    }

    private boolean validateCommand(Session session, UserGameCommand command) {
        if (command.getAuthToken() == null || command.getAuthToken().trim().isEmpty()) {
            sendError(session, "Error: Invalid auth token");
            return false;
        }
        if (command.getGameID() == null) {
            sendError(session, "Error: Invalid game ID");
            return false;
        }
        return true;
    }

    private String validateAndGetUsername(Session session, UserGameCommand command) throws DataAccessException {
        var authData = dataAccess.getAuth(command.getAuthToken());
        if (authData == null) {
            sendError(session, "Error: Invalid auth token");
            return null;
        }
        return authData.getUsername();
    }

    private GameData validateAndGetGame(Session session, UserGameCommand command) throws DataAccessException {
        GameData gameData = dataAccess.getGame(command.getGameID());
        if (gameData == null) {
            sendError(session, "Error: Game not found");
            return null;
        }
        return gameData;
    }

    private ChessMove extractMoveFromMessage(Session session) {
        try {
            String originalMessage = SESSION_MESSAGES.get(session);
            if (originalMessage != null) {
                com.google.gson.JsonObject jsonObject = gson.fromJson(originalMessage, com.google.gson.JsonObject.class);
                if (jsonObject.has("move")) {
                    return gson.fromJson(jsonObject.get("move"), ChessMove.class);
                }
            }
            sendError(session, "Error: Invalid move command");
            return null;
        } catch (Exception e) {
            sendError(session, "Error: Invalid move format");
            return null;
        }
    }

    private ChessGame.TeamColor validatePlayerTurn(Session session, String username, GameData gameData, ChessGame game) {
        ChessGame.TeamColor playerColor = null;
        if (username.equals(gameData.getWhiteUsername())) {
            playerColor = ChessGame.TeamColor.WHITE;
        } else if (username.equals(gameData.getBlackUsername())) {
            playerColor = ChessGame.TeamColor.BLACK;
        } else {
            sendError(session, "Error: Observer cannot make moves");
            return null;
        }

        if (game.getTeamTurn() != playerColor) {
            sendError(session, "Error: Not your turn");
            return null;
        }
        return playerColor;
    }

    private void executeMoveAndUpdate(Session session, UserGameCommand command, ChessGame game,
                                      GameData gameData, ChessMove move, String username,
                                      ChessGame.TeamColor playerColor) throws DataAccessException {
        try {
            game.makeMove(move);
        } catch (InvalidMoveException e) {
            sendError(session, "Error: Invalid move - " + e.getMessage());
            return;
        }

        GameData updatedGameData = new GameData(gameData.getGameID(), gameData.getWhiteUsername(),
                gameData.getBlackUsername(), gameData.getGameName(), game);
        dataAccess.updateGame(updatedGameData);

        LoadGameMessage loadMessage = new LoadGameMessage(game);
        broadcastToAll(command.getGameID(), loadMessage);

        String moveDescription = formatMove(move, playerColor);
        NotificationMessage moveNotification = new NotificationMessage(username + " made move: " + moveDescription);
        broadcastToOthers(command.getGameID(), session, moveNotification);

        checkGameEndConditions(command, game, gameData, playerColor);
    }

    private void checkGameEndConditions(UserGameCommand command, ChessGame game, GameData gameData, ChessGame.TeamColor playerColor) {
        ChessGame.TeamColor oppositeColor = (playerColor == ChessGame.TeamColor.WHITE) ?
                ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;

        if (game.isInCheckmate(oppositeColor)) {
            String opponentUsername = (oppositeColor == ChessGame.TeamColor.WHITE) ?
                    gameData.getWhiteUsername() : gameData.getBlackUsername();
            NotificationMessage checkmateNotification = new NotificationMessage(opponentUsername + " is in checkmate");
            broadcastToAll(command.getGameID(), checkmateNotification);
        } else if (game.isInCheck(oppositeColor)) {
            String opponentUsername = (oppositeColor == ChessGame.TeamColor.WHITE) ?
                    gameData.getWhiteUsername() : gameData.getBlackUsername();
            NotificationMessage checkNotification = new NotificationMessage(opponentUsername + " is in check");
            broadcastToAll(command.getGameID(), checkNotification);
        } else if (game.isInStalemate(oppositeColor)) {
            NotificationMessage stalemateNotification = new NotificationMessage("Game ended in stalemate");
            broadcastToAll(command.getGameID(), stalemateNotification);
        }
    }

    private record SessionInfo(String username, Integer gameID, String role) {}
}