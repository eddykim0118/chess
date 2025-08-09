package server.websocket;

import chess.ChessGame;
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
        if (info != null) {
            CopyOnWriteArraySet<Session> sessions = gameSessions.get(info.gameID());
            if (sessions != null) {
                sessions.remove(session);
            }
        }
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws Exception {
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
            // Validate auth token and get username
            String username = dataAccess.getAuth(command.getAuthToken()).username();

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
            if (username.equals(gameData.whiteUsername())) {
                role = username + " joined as white player";
            } else if (username.equals(gameData.blackUsername())) {
                role = username + " joined as black player";
            } else {
                role = username + " joined as observer";
            }

            sessionInfo.put(session, new SessionInfo(username, command.getGameID(), role));

            // Send LOAD_GAME to connecting client
            LoadGameMessage loadMessage = new LoadGameMessage(gameData.game());
            sendMessage(session, loadMessage);

            // Send NOTIFICATION to other clients
            NotificationMessage notification = new NotificationMessage(role);
            broadcastToOthers(command.getGameID(), session, notification);

        } catch (DataAccessException e) {
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private void handleMakeMove(Session session, UserGameCommand command) {
        // TODO: Implement make move logic
        System.out.println("Handling MAKE_MOVE command");
    }

    private void handleLeave(Session session, UserGameCommand command) {
        // TODO: Implement leave logic
        System.out.println("Handling LEAVE command");
    }

    private void handleResign(Session session, UserGameCommand command) {
        // TODO: Implement resign logic
        System.out.println("Handling RESIGN command");
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