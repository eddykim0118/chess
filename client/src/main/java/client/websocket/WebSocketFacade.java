package client.websocket;

import chess.ChessGame;
import com.google.gson.Gson;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;

@ClientEndpoint
public class WebSocketFacade extends Endpoint {

    private final Gson gson = new Gson();
    private Session session;
    private NotificationHandler notificationHandler;

    public interface NotificationHandler {
        void notify(ServerMessage message);
    }

    public WebSocketFacade(String url, NotificationHandler notificationHandler) throws Exception {
        try {
            this.notificationHandler = notificationHandler;
            URI socketURI = new URI(url.replace("http", "ws") + "/ws");

            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            this.session = container.connectToServer(this, socketURI);

            // Set message handler
            this.session.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }
            });
        } catch (Exception ex) {
            throw new Exception("Failed to connect to WebSocket: " + ex.getMessage());
        }
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        // Connection opened
    }

    public void connectToGame(String authToken, Integer gameID) throws IOException {
        try {
            UserGameCommand connectCommand = new UserGameCommand(UserGameCommand.CommandType.CONNECT, authToken, gameID);
            this.session.getBasicRemote().sendText(gson.toJson(connectCommand));
        } catch (IOException ex) {
            throw new IOException("Failed to send connect command: " + ex.getMessage());
        }
    }

    public void leaveGame(String authToken, Integer gameID) throws IOException {
        try {
            UserGameCommand leaveCommand = new UserGameCommand(UserGameCommand.CommandType.LEAVE, authToken, gameID);
            this.session.getBasicRemote().sendText(gson.toJson(leaveCommand));
        } catch (IOException ex) {
            throw new IOException("Failed to send leave command: " + ex.getMessage());
        }
    }

    public void resignGame(String authToken, Integer gameID) throws IOException {
        try {
            UserGameCommand resignCommand = new UserGameCommand(UserGameCommand.CommandType.RESIGN, authToken, gameID);
            this.session.getBasicRemote().sendText(gson.toJson(resignCommand));
        } catch (IOException ex) {
            throw new IOException("Failed to send resign command: " + ex.getMessage());
        }
    }

    private void handleMessage(String message) {
        try {
            ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);

            switch (serverMessage.getServerMessageType()) {
                case LOAD_GAME -> {
                    LoadGameMessage loadGameMessage = gson.fromJson(message, LoadGameMessage.class);
                    notificationHandler.notify(loadGameMessage);
                }
                case ERROR -> {
                    ErrorMessage errorMessage = gson.fromJson(message, ErrorMessage.class);
                    notificationHandler.notify(errorMessage);
                }
                case NOTIFICATION -> {
                    NotificationMessage notificationMessage = gson.fromJson(message, NotificationMessage.class);
                    notificationHandler.notify(notificationMessage);
                }
            }
        } catch (Exception ex) {
            System.err.println("Error handling WebSocket message: " + ex.getMessage());
        }
    }

    public void close() {
        try {
            this.session.close();
        } catch (IOException ex) {
            System.err.println("Error closing WebSocket: " + ex.getMessage());
        }
    }
}