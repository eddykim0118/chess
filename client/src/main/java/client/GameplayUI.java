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
        System.out.println("  help - Display this help information");
        System.out.println("  redraw - Redraw the chess board");
        System.out.println("  leave - Remove yourself from the game and return to main menu");

        if (!isObserver) {
            System.out.println("  move - Make a chess move (format: e2 e4)");
            System.out.println("  resign - Forfeit the game");
        }

        System.out.println("  highlight - Show legal moves for a piece (format: e2)");
        System.out.println("\nYou are playing as: " +
                (isObserver ? "Observer" : playerColor.toString() + " player"));
    }

    private void redrawBoard() {
        if (currentGame != null) {
            System.out.println("\nCurrent game state:");
            drawBoard(currentGame.getBoard());

            // Show whose turn it is
            ChessGame.TeamColor currentTurn = currentGame.getTeamTurn();
            System.out.println("Current turn: " + currentTurn);

            // Check for game status
            if (currentGame.isInCheck(ChessGame.TeamColor.WHITE)) {
                System.out.println("White is in check!");
            }
            if (currentGame.isInCheck(ChessGame.TeamColor.BLACK)) {
                System.out.println("Black is in check!");
            }
            if (currentGame.isInCheckmate(currentTurn)) {
                System.out.println("Checkmate! Game is over.");
            }
            if (currentGame.isInStalemate(currentTurn)) {
                System.out.println("Stalemate! Game is over.");
            }
        } else {
            System.out.println("No game loaded yet. Waiting for game data...");
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
        System.out.println("   a  b  c  d  e  f  g  h");
        System.out.println("  ┌──┬──┬──┬──┬──┬──┬──┬──┐");

        for (int row = 8; row >= 1; row--) {
            System.out.print(row + " │");
            for (int col = 1; col <= 8; col++) {
                ChessPiece piece = board.getPiece(new ChessPosition(row, col));
                System.out.print(getPieceSymbol(piece) + " │");
            }
            System.out.println(" " + row);

            if (row > 1) {
                System.out.println("  ├──┼──┼──┼──┼──┼──┼──┼──┤");
            }
        }

        System.out.println("  └──┴──┴──┴──┴──┴──┴──┴──┘");
        System.out.println("   a  b  c  d  e  f  g  h");
    }

    private void drawBoardFromBlackPerspective(ChessBoard board) {
        System.out.println("   h  g  f  e  d  c  b  a");
        System.out.println("  ┌──┬──┬──┬──┬──┬──┬──┬──┐");

        for (int row = 1; row <= 8; row++) {
            System.out.print(row + " │");
            for (int col = 8; col >= 1; col--) {
                ChessPiece piece = board.getPiece(new ChessPosition(row, col));
                System.out.print(getPieceSymbol(piece) + " │");
            }
            System.out.println(" " + row);

            if (row < 8) {
                System.out.println("  ├──┼──┼──┼──┼──┼──┼──┼──┤");
            }
        }

        System.out.println("  └──┴──┴──┴──┴──┴──┴──┴──┘");
        System.out.println("   h  g  f  e  d  c  b  a");
    }

    private String getPieceSymbol(ChessPiece piece) {
        if (piece == null) {
            return " ";
        }

        String symbol = switch (piece.getPieceType()) {
            case PAWN -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "♙" : "♟";
            case ROOK -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "♖" : "♜";
            case KNIGHT -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "♘" : "♞";
            case BISHOP -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "♗" : "♝";
            case QUEEN -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "♕" : "♛";
            case KING -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? "♔" : "♚";
        };

        return symbol;
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

        if (currentGame == null) {
            System.out.println("No game loaded.");
            return;
        }

        // Check if it's the player's turn
        if (currentGame.getTeamTurn() != playerColor) {
            System.out.println("It's not your turn.");
            return;
        }

        System.out.print("Enter your move (e.g., 'e2 e4' or 'e7 e8 Q' for promotion): ");
        String moveInput = scanner.nextLine().trim();

        try {
            ChessMove move = parseMove(moveInput);
            if (move == null) {
                System.out.println("Invalid move format. Use format like 'e2 e4' or 'e7 e8 Q' for promotion.");
                return;
            }

            webSocket.makeMove(authToken, gameID, move);

        } catch (IOException e) {
            System.out.println("Error sending move: " + e.getMessage());
        }
    }

    private ChessMove parseMove(String input) {
        String[] parts = input.split("\\s+");

        if (parts.length < 2 || parts.length > 3) {
            return null;
        }

        try {
            ChessPosition start = parsePosition(parts[0]);
            ChessPosition end = parsePosition(parts[1]);

            if (start == null || end == null) {
                return null;
            }

            ChessPiece.PieceType promotionPiece = null;
            if (parts.length == 3) {
                promotionPiece = parsePromotionPiece(parts[2]);
                if (promotionPiece == null) {
                    return null;
                }
            }

            return new ChessMove(start, end, promotionPiece);

        } catch (Exception e) {
            return null;
        }
    }

    private ChessPosition parsePosition(String pos) {
        if (pos.length() != 2) {
            return null;
        }

        char colChar = pos.charAt(0);
        char rowChar = pos.charAt(1);

        if (colChar < 'a' || colChar > 'h' || rowChar < '1' || rowChar > '8') {
            return null;
        }

        int col = colChar - 'a' + 1; // a=1, b=2, ..., h=8
        int row = rowChar - '1' + 1; // 1=1, 2=2, ..., 8=8

        return new ChessPosition(row, col);
    }

    private ChessPiece.PieceType parsePromotionPiece(String piece) {
        return switch (piece.toUpperCase()) {
            case "Q" -> ChessPiece.PieceType.QUEEN;
            case "R" -> ChessPiece.PieceType.ROOK;
            case "B" -> ChessPiece.PieceType.BISHOP;
            case "N" -> ChessPiece.PieceType.KNIGHT;
            default -> null;
        };
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