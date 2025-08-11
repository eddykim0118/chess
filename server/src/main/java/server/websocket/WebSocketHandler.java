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

    // Track sessions by gameID
    private static final ConcurrentHashMap<Integer, CopyOnWriteArraySet<Session>> gameSessions = new ConcurrentHashMap<>();
    // Track session to user info
    private static final ConcurrentHashMap<Session, SessionInfo> sessionInfo = new ConcurrentHashMap<>();
    // Track resigned games
    private static final ConcurrentHashMap<Integer, Boolean> resignedGames = new ConcurrentHashMap<>();
    // Track original messages by session
    private static final ConcurrentHashMap<Session, String> sessionMessages = new ConcurrentHashMap<>();

    // Static method to set DataAccess (called from Server)
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
        // Clean up session tracking
        SessionInfo info = sessionInfo.remove(session);
        sessionMessages.remove(session);
        if (info != null) {
            CopyOnWriteArraySet<Session> sessions = gameSessions.get(info.gameID());
            if (sessions != null) {
                sessions.remove(session);
            }
        }
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws Exception {
        // Store the original message for potential re-parsing
        sessionMessages.put(session, message);

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
            // Validate auth token exists
            if (command.getAuthToken() == null || command.getAuthToken().trim().isEmpty()) {
                sendError(session, "Error: Invalid auth token");
                return;
            }

            // Validate gameID exists
            if (command.getGameID() == null) {
                sendError(session, "Error: Invalid game ID");
                return;
            }

            // Validate auth token and get username
            var authData = dataAccess.getAuth(command.getAuthToken());
            if (authData == null) {
                sendError(session, "Error: Invalid auth token");
                return;
            }
            String username = authData.getUsername();

            // Validate game exists
            GameData gameData = dataAccess.getGame(command.getGameID());
            if (gameData == null) {
                sendError(session, "Error: Game not found");
                return;
            }

            // Track this session
            gameSessions.computeIfAbsent(command.getGameID(), k -> new CopyOnWriteArraySet<>()).add(session);

            // Determine player role
            String role;
            if (username.equals(gameData.getWhiteUsername())) {
                role = username + " joined as white player";
            } else if (username.equals(gameData.getBlackUsername())) {
                role = username + " joined as black player";
            } else {
                role = username + " joined as observer";
            }

            sessionInfo.put(session, new SessionInfo(username, command.getGameID(), role));

            // Send LOAD_GAME to connecting client
            LoadGameMessage loadMessage = new LoadGameMessage(gameData.getGame());
            sendMessage(session, loadMessage);

            // Send NOTIFICATION to other clients
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
            // Check if game is resigned
            if (resignedGames.getOrDefault(command.getGameID(), false)) {
                sendError(session, "Error: Game is over due to resignation");
                return;
            }

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

            // Check if game is over
            if (isGameOver(command.getGameID(), game)) {
                sendError(session, "Error: Game is over");
                return;
            }

            // Parse the move from the command
            ChessMove move = null;
            try {
                // Parse the original message to get the move
                String originalMessage = sessionMessages.get(session);
                if (originalMessage != null) {
                    com.google.gson.JsonObject jsonObject = gson.fromJson(originalMessage, com.google.gson.JsonObject.class);
                    if (jsonObject.has("move")) {
                        move = gson.fromJson(jsonObject.get("move"), ChessMove.class);
                    }
                }

                if (move == null) {
                    sendError(session, "Error: Invalid move command");
                    return;
                }
            } catch (Exception e) {
                sendError(session, "Error: Invalid move format");
                return;
            }

            // Verify player authorization
            ChessGame.TeamColor playerColor = null;
            if (username.equals(gameData.getWhiteUsername())) {
                playerColor = ChessGame.TeamColor.WHITE;
            } else if (username.equals(gameData.getBlackUsername())) {
                playerColor = ChessGame.TeamColor.BLACK;
            } else {
                sendError(session, "Error: Observer cannot make moves");
                return;
            }

            // Check if it's the player's turn
            if (game.getTeamTurn() != playerColor) {
                sendError(session, "Error: Not your turn");
                return;
            }

            // Validate and make the move
            try {
                game.makeMove(move);
            } catch (InvalidMoveException e) {
                sendError(session, "Error: Invalid move - " + e.getMessage());
                return;
            }

            // Update game in database
            GameData updatedGameData = new GameData(gameData.getGameID(), gameData.getWhiteUsername(),
                    gameData.getBlackUsername(), gameData.getGameName(), game);
            dataAccess.updateGame(updatedGameData);

            // Send LOAD_GAME to all clients
            LoadGameMessage loadMessage = new LoadGameMessage(game);
            broadcastToAll(command.getGameID(), loadMessage);

            // Send move notification to other clients
            String moveDescription = formatMove(move, playerColor);
            NotificationMessage moveNotification = new NotificationMessage(username + " made move: " + moveDescription);
            broadcastToOthers(command.getGameID(), session, moveNotification);

            // Check for check/checkmate/stalemate and notify all clients
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

        } catch (DataAccessException e) {
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private void handleLeave(Session session, UserGameCommand command) {
        try {
            // Validate auth token and gameID
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

            // Get game data
            GameData gameData = dataAccess.getGame(command.getGameID());
            if (gameData == null) {
                sendError(session, "Error: Game not found");
                return;
            }

            // Determine if this is a player leaving (vs observer)
            boolean isPlayer = username.equals(gameData.getWhiteUsername()) ||
                    username.equals(gameData.getBlackUsername());

            if (isPlayer) {
                // Remove player from game
                String newWhiteUsername = username.equals(gameData.getWhiteUsername()) ? null : gameData.getWhiteUsername();
                String newBlackUsername = username.equals(gameData.getBlackUsername()) ? null : gameData.getBlackUsername();

                GameData updatedGameData = new GameData(gameData.getGameID(), newWhiteUsername,
                        newBlackUsername, gameData.getGameName(), gameData.getGame());
                dataAccess.updateGame(updatedGameData);
            }

            // Remove session from tracking
            CopyOnWriteArraySet<Session> sessions = gameSessions.get(command.getGameID());
            if (sessions != null) {
                sessions.remove(session);
            }
            sessionInfo.remove(session);
            sessionMessages.remove(session);

            // Send notification to OTHER clients (not the leaving client)
            NotificationMessage notification = new NotificationMessage(username + " left the game");
            broadcastToOthers(command.getGameID(), session, notification);

        } catch (DataAccessException e) {
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private void handleResign(Session session, UserGameCommand command) {
        try {
            // Validate auth token and gameID
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

            // Check if game is already over
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

            // Mark game as resigned
            resignedGames.put(command.getGameID(), true);

            // Send resignation notification to ALL clients
            NotificationMessage resignNotification = new NotificationMessage(username + " resigned. Game is over.");
            broadcastToAll(command.getGameID(), resignNotification);

        } catch (DataAccessException e) {
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private boolean isGameOver(Integer gameID, ChessGame game) {
        // Check if game was resigned
        if (resignedGames.getOrDefault(gameID, false)) {
            return true;
        }

        // Check for checkmate or stalemate
        return game.isInCheckmate(ChessGame.TeamColor.WHITE) ||
                game.isInCheckmate(ChessGame.TeamColor.BLACK) ||
                game.isInStalemate(ChessGame.TeamColor.WHITE) ||
                game.isInStalemate(ChessGame.TeamColor.BLACK);
    }

    private void broadcastToAll(Integer gameID, Object message) {
        CopyOnWriteArraySet<Session> sessions = gameSessions.get(gameID);
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
        CopyOnWriteArraySet<Session> sessions = gameSessions.get(gameID);
        if (sessions != null) {
            for (Session session : sessions) {
                if (!session.equals(excludeSession)) {
                    sendMessage(session, message);
                }
            }
        }
    }

    private record SessionInfo(String username, Integer gameID, String role) {}
}