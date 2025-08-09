package server.websocket;

import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

@WebSocket
public class WebSocketHandler {

    private final Gson gson = new Gson();

    @OnWebSocketConnect
    public void onConnect(Session session) throws Exception {
        System.out.println("WebSocket connection established");
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("WebSocket connection closed");
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
        // TODO: Implement connect logic
        System.out.println("Handling CONNECT command");
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
}