package client;

import chess.*;
import client.websocket.WebSocketFacade;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import java.io.IOException;
import java.util.Scanner;

public class GameplayUI implements WebSocketFacade.NotificationHandler {

    private final Scanner scanner;
    private final String serverUrl;
    private final String authToken;
    private final String username;
    private final Integer gameID;
    private final ChessGame.TeamColor playerColor;
    private WebSocketFacade webSocket;
    private ChessGame currentGame;
    private boolean isObserver;

    public GameplayUI(Scanner scanner, String serverUrl, String authToken, String username,
                      Integer gameID, ChessGame.TeamColor playerColor) {
        this.scanner = scanner;
        this.serverUrl = serverUrl;
        this.authToken = authToken;
        this.username = username;
        this.gameID = gameID;
        this.playerColor = playerColor;
        this.isObserver = (playerColor == null);
    }

    public void start() {
        try {
            // Connect to WebSocket
            webSocket = new WebSocketFacade(serverUrl, this);

            // Send CONNECT command
            webSocket.connectToGame(authToken, gameID);

            System.out.println("Connected to game. Type 'help' for available commands.");

            // Start command loop
            gameplayLoop();

        } catch (Exception e) {
            System.out.println("Failed to connect to game: " + e.getMessage());
        }
    }

    private void gameplayLoop() {
        boolean running = true;
        while (running) {
            System.out.print("\n>>> ");
            String input = scanner.nextLine().trim().toLowerCase();

            switch (input) {
                case "help" -> showHelp();
                case "redraw" -> redrawBoard();
                case "leave" -> {
                    running = false;
                    leaveGame();
                }
                case "resign" -> resignGame();
                case "move" -> makeMove();
                case "highlight" -> highlightMoves();
                default -> System.out.println("Unknown command. Type 'help' for available commands.");
            }
        }
    }

    private void showHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("  help - Show this help message");
        System.out.println("  redraw - Redraw the chess board");
        System.out.println("  leave - Leave the game");
        if (!isObserver) {
            System.out.println("  resign - Resign from the game");
            System.out.println("  move - Make a move");
        }
        System.out.println("  highlight - Highlight legal moves for a piece");
    }

    private void redrawBoard() {
        if (currentGame != null) {
            drawBoard(currentGame.getBoard());
        } else {
            System.out.println("No game loaded yet.");
        }
    }

    private void drawBoard(ChessBoard board) {
        System.out.println();

        if (playerColor == ChessGame.TeamColor.BLACK) {
            // Draw board from black perspective (black pieces on bottom)
            drawBoardFromBlackPerspective(board);
        } else {
            // Draw board from white perspective or observer (white pieces on bottom)
            drawBoardFromWhitePerspective(board);
        }
    }

    private void drawBoardFromWhitePerspective(ChessBoard board) {
        System.out.println("  a b c d e f g h");
        for (int row = 8; row >= 1; row--) {
            System.out.print(row + " ");
            for (int col = 1; col <= 8; col++) {
                ChessPiece piece = board.getPiece(new ChessPosition(row, col));
                System.out.print(getPieceSymbol(piece) + " ");
            }
            System.out.println(row);
        }
        System.out.println("  a b c d e f g h");
    }

    private void drawBoardFromBlackPerspective(ChessBoard board) {
        System.out.println("  h g f e d c b a");
        for (int row = 1; row <= 8; row++) {
            System.out.print(row + " ");
            for (int col = 8; col >= 1; col--) {
                ChessPiece piece = board.getPiece(new ChessPosition(row, col));
                System.out.print(getPieceSymbol(piece) + " ");
            }
            System.out.println(row);
        }
        System.out.println("  h g f e d c b a");
    }

    private String getPieceSymbol(ChessPiece piece) {
        if (piece == null) {
            return ".";
        }

        String symbol = switch (piece.getPieceType()) {
            case PAWN -> "P";
            case ROOK -> "R";
            case KNIGHT -> "N";
            case BISHOP -> "B";
            case QUEEN -> "Q";
            case KING -> "K";
        };

        return piece.getTeamColor() == ChessGame.TeamColor.WHITE ?
                symbol.toUpperCase() : symbol.toLowerCase();
    }

    private void leaveGame() {
        try {
            webSocket.leaveGame(authToken, gameID);
            webSocket.close();
            System.out.println("Left the game. Returning to main menu.");
        } catch (IOException e) {
            System.out.println("Error leaving game: " + e.getMessage());
        }
    }

    private void resignGame() {
        if (isObserver) {
            System.out.println("Observers cannot resign.");
            return;
        }

        System.out.print("Are you sure you want to resign? (yes/no): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();

        if (confirmation.equals("yes") || confirmation.equals("y")) {
            try {
                webSocket.resignGame(authToken, gameID);
                System.out.println("You have resigned from the game.");
            } catch (IOException e) {
                System.out.println("Error resigning: " + e.getMessage());
            }
        } else {
            System.out.println("Resignation cancelled.");
        }
    }

    private void makeMove() {
        if (isObserver) {
            System.out.println("Observers cannot make moves.");
            return;
        }

        System.out.println("Move functionality not yet implemented.");
        // TODO: Implement move input and sending
    }

    private void highlightMoves() {
        System.out.println("Highlight moves functionality not yet implemented.");
        // TODO: Implement highlighting legal moves
    }

    @Override
    public void notify(ServerMessage message) {
        switch (message.getServerMessageType()) {
            case LOAD_GAME -> {
                LoadGameMessage loadMessage = (LoadGameMessage) message;
                currentGame = (ChessGame) loadMessage.getGame();
                System.out.println("\nGame updated:");
                drawBoard(currentGame.getBoard());
            }
            case ERROR -> {
                ErrorMessage errorMessage = (ErrorMessage) message;
                System.out.println("\nError: " + errorMessage.getErrorMessage());
            }
            case NOTIFICATION -> {
                NotificationMessage notification = (NotificationMessage) message;
                System.out.println("\n" + notification.getMessage());
            }
        }
        System.out.print(">>> ");
    }
}