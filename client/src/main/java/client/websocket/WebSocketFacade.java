package client.websocket;

import chess.ChessGame;
import chess.ChessMove;
import client.EscapeSequences;

import com.google.gson.Gson;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;
import websocket.commands.MakeMoveCommand;

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
            // First parse to get the message type
            com.google.gson.JsonObject jsonObject = gson.fromJson(message, com.google.gson.JsonObject.class);
            String messageType = jsonObject.get("serverMessageType").getAsString();

            ServerMessage serverMessage;

            switch (messageType) {
                case "LOAD_GAME" -> {
                    serverMessage = gson.fromJson(message, LoadGameMessage.class);
                }
                case "ERROR" -> {
                    serverMessage = gson.fromJson(message, ErrorMessage.class);
                }
                case "NOTIFICATION" -> {
                    serverMessage = gson.fromJson(message, NotificationMessage.class);
                }
                default -> {
                    System.err.println("Unknown message type: " + messageType);
                    return;
                }
            }

            notificationHandler.notify(serverMessage);

        } catch (Exception ex) {
            System.err.println("Error handling WebSocket message: " + ex.getMessage());
            System.err.println("Message was: " + message);
        }
    }

    public void makeMove(String authToken, Integer gameID, ChessMove move) throws IOException {
        try {
            MakeMoveCommand moveCommand = new MakeMoveCommand(authToken, gameID, move);
            this.session.getBasicRemote().sendText(gson.toJson(moveCommand));
        } catch (IOException ex) {
            throw new IOException("Failed to send move command: " + ex.getMessage());
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