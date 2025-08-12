package server.websocket;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import chess.InvalidMoveException;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
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

    // Extracted validation methods to eliminate duplication
    private ValidationResult validateCommand(Session session, UserGameCommand command) {
        if (command.getAuthToken() == null || command.getAuthToken().trim().isEmpty()) {
            sendError(session, "Error: Invalid auth token");
            return ValidationResult.FAILED;
        }

        if (command.getGameID() == null) {
            sendError(session, "Error: Invalid game ID");
            return ValidationResult.FAILED;
        }

        try {
            var authData = dataAccess.getAuth(command.getAuthToken());
            if (authData == null) {
                sendError(session, "Error: Invalid auth token");
                return ValidationResult.FAILED;
            }

            GameData gameData = dataAccess.getGame(command.getGameID());
            if (gameData == null) {
                sendError(session, "Error: Game not found");
                return ValidationResult.FAILED;
            }

            return new ValidationResult(authData.getUsername(), gameData);
        } catch (DataAccessException e) {
            sendError(session, "Error: " + e.getMessage());
            return ValidationResult.FAILED;
        }
    }

    private void handleConnect(Session session, UserGameCommand command) {
        try {
            ValidationResult validation = validateCommand(session, command);
            if (validation.isFailed()) return;

            String username = validation.username();
            GameData gameData = validation.gameData();

            GAME_SESSIONS.computeIfAbsent(command.getGameID(), k -> new CopyOnWriteArraySet<>()).add(session);

            String role = determineUserRole(username, gameData);
            SESSION_INFO.put(session, new SessionInfo(username, command.getGameID(), role));

            LoadGameMessage loadMessage = new LoadGameMessage(gameData.getGame());
            sendMessage(session, loadMessage);
            NotificationMessage notification = new NotificationMessage(role);
            broadcastToOthers(command.getGameID(), session, notification);

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

            ValidationResult validation = validateCommand(session, command);
            if (validation.isFailed()) return;

            String username = validation.username();
            GameData gameData = validation.gameData();
            ChessGame game = gameData.getGame();

            if (isGameOver(command.getGameID(), game)) {
                sendError(session, "Error: Game is over");
                return;
            }

            ChessMove move = extractMoveFromMessage(session);
            if (move == null) return;

            ChessGame.TeamColor playerColor = getPlayerColor(username, gameData);
            if (playerColor == null) {
                sendError(session, "Error: Observer cannot make moves");
                return;
            }

            if (game.getTeamTurn() != playerColor) {
                sendError(session, "Error: Not your turn");
                return;
            }

            executeMoveAndUpdate(session, command, game, gameData, move, username, playerColor);

        } catch (DataAccessException e) {
            sendError(session, "Error: " + e.getMessage());
        }
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

        checkGameEndConditions(command.getGameID(), game, gameData, playerColor);
    }

    private void checkGameEndConditions(Integer gameID, ChessGame game, GameData gameData, 
                                       ChessGame.TeamColor playerColor) {
        ChessGame.TeamColor oppositeColor = (playerColor == ChessGame.TeamColor.WHITE) ?
                ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;

        if (game.isInCheckmate(oppositeColor)) {
            String opponentUsername = getOpponentUsername(gameData, oppositeColor);
            NotificationMessage checkmateNotification = new NotificationMessage(opponentUsername + " is in checkmate");
            broadcastToAll(gameID, checkmateNotification);
        } else if (game.isInCheck(oppositeColor)) {
            String opponentUsername = getOpponentUsername(gameData, oppositeColor);
            NotificationMessage checkNotification = new NotificationMessage(opponentUsername + " is in check");
            broadcastToAll(gameID, checkNotification);
        } else if (game.isInStalemate(oppositeColor)) {
            NotificationMessage stalemateNotification = new NotificationMessage("Game ended in stalemate");
            broadcastToAll(gameID, stalemateNotification);
        }
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

    private ChessGame.TeamColor getPlayerColor(String username, GameData gameData) {
        if (username.equals(gameData.getWhiteUsername())) {
            return ChessGame.TeamColor.WHITE;
        } else if (username.equals(gameData.getBlackUsername())) {
            return ChessGame.TeamColor.BLACK;
        }
        return null;
    }

    private String getOpponentUsername(GameData gameData, ChessGame.TeamColor color) {
        return (color == ChessGame.TeamColor.WHITE) ? 
               gameData.getWhiteUsername() : gameData.getBlackUsername();
    }

    private String determineUserRole(String username, GameData gameData) {
        if (username.equals(gameData.getWhiteUsername())) {
            return username + " joined as white player";
        } else if (username.equals(gameData.getBlackUsername())) {
            return username + " joined as black player";
        } else {
            return username + " joined as observer";
        }
    }

    private void handleLeave(Session session, UserGameCommand command) {
        ValidationResult validation = validateCommand(session, command);
        if (validation.isFailed()) return;

        String username = validation.username();
        GameData gameData = validation.gameData();

        boolean isPlayer = username.equals(gameData.getWhiteUsername()) ||
                username.equals(gameData.getBlackUsername());

        if (isPlayer) {
            try {
                updateGameDataOnPlayerLeave(username, gameData);
            } catch (DataAccessException e) {
                sendError(session, "Error: " + e.getMessage());
                return;
            }
        }

        cleanupSession(session, command.getGameID());
        NotificationMessage notification = new NotificationMessage(username + " left the game");
        broadcastToOthers(command.getGameID(), session, notification);
    }

    private void updateGameDataOnPlayerLeave(String username, GameData gameData) throws DataAccessException {
        String newWhiteUsername = username.equals(gameData.getWhiteUsername()) ? null : gameData.getWhiteUsername();
        String newBlackUsername = username.equals(gameData.getBlackUsername()) ? null : gameData.getBlackUsername();

        GameData updatedGameData = new GameData(gameData.getGameID(), newWhiteUsername,
                newBlackUsername, gameData.getGameName(), gameData.getGame());
        dataAccess.updateGame(updatedGameData);
    }

    private void cleanupSession(Session session, Integer gameID) {
        CopyOnWriteArraySet<Session> sessions = GAME_SESSIONS.get(gameID);
        if (sessions != null) {
            sessions.remove(session);
        }
        SESSION_INFO.remove(session);
        SESSION_MESSAGES.remove(session);
    }

    private void handleResign(Session session, UserGameCommand command) {
        ValidationResult validation = validateCommand(session, command);
        if (validation.isFailed()) return;

        String username = validation.username();
        GameData gameData = validation.gameData();
        ChessGame game = gameData.getGame();

        if (isGameOver(command.getGameID(), game)) {
            sendError(session, "Error: Game is already over");
            return;
        }

        if (!isPlayer(username, gameData)) {
            sendError(session, "Error: Observer cannot resign");
            return;
        }

        RESIGNED_GAMES.put(command.getGameID(), true);
        NotificationMessage resignNotification = new NotificationMessage(username + " resigned. Game is over.");
        broadcastToAll(command.getGameID(), resignNotification);
    }

    private boolean isPlayer(String username, GameData gameData) {
        return username.equals(gameData.getWhiteUsername()) || username.equals(gameData.getBlackUsername());
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

    // Helper classes and records
    private record SessionInfo(String username, Integer gameID, String role) {}

    private static class ValidationResult {
        private static final ValidationResult FAILED = new ValidationResult();
        
        private final String username;
        private final GameData gameData;
        private final boolean failed;

        private ValidationResult() {
            this.username = null;
            this.gameData = null;
            this.failed = true;
        }

        public ValidationResult(String username, GameData gameData) {
            this.username = username;
            this.gameData = gameData;
            this.failed = false;
        }

        public boolean isFailed() { return failed; }
        public String username() { return username; }
        public GameData gameData() { return gameData; }
    }
}