package client;

import chess.*;
import client.websocket.WebSocketFacade;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import java.io.IOException;
import java.util.Scanner;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

public class GameplayUI implements WebSocketFacade.NotificationHandler {

    public interface GameplayCallback {
        void onGameplayExit();
    }

    private final Scanner scanner;
    private final String serverUrl;
    private final String authToken;
    private final String username;
    private final Integer gameID;
    private final ChessGame.TeamColor playerColor;
    private final GameplayCallback callback;
    private WebSocketFacade webSocket;
    private ChessGame currentGame;
    private boolean isObserver;

    public GameplayUI(Scanner scanner, String serverUrl, String authToken, String username,
                      Integer gameID, ChessGame.TeamColor playerColor, GameplayCallback callback) {
        this.scanner = scanner;
        this.serverUrl = serverUrl;
        this.authToken = authToken;
        this.username = username;
        this.gameID = gameID;
        this.playerColor = playerColor;
        this.isObserver = (playerColor == null);
        this.callback = callback;
    }

    // Keep the old constructor for backward compatibility
    public GameplayUI(Scanner scanner, String serverUrl, String authToken, String username,
                      Integer gameID, ChessGame.TeamColor playerColor) {
        this(scanner, serverUrl, authToken, username, gameID, playerColor, null);
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
                    System.out.print("Are you sure you want to leave the game? (yes/no): ");
                    String confirmation = scanner.nextLine().trim().toLowerCase();

                    if (confirmation.equals("yes") || confirmation.equals("y")) {
                        running = false;
                        leaveGame();
                    } else {
                        System.out.println("Cancelled leaving the game.");
                    }
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
            System.out.println("Leaving game...");
            webSocket.leaveGame(authToken, gameID);

            Thread.sleep(100);
            webSocket.close();

            System.out.println("Left the game successfully.");

            if (callback != null) {
                callback.onGameplayExit();
            }

        } catch (IOException e) {
            System.out.println("Error leaving game: " + e.getMessage());
            webSocket.close();
        } catch (InterruptedException e) {
            System.out.println("Interrupted while leaving game.");
            webSocket.close();
        }
    }

    private void resignGame() {
        if (isObserver) {
            System.out.println("Observers cannot resign.");
            return;
        }

        if (isGameOver()) {
            System.out.println("Game is already over - cannot resign.");
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

        if (isGameOver()) {
            System.out.println("Game is over - no more moves allowed.");
            return;
        }

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

            ChessPiece piece = currentGame.getBoard().getPiece(move.getStartPosition());
            if (piece == null) {
                System.out.println("No piece at starting position.");
                return;
            }

            if (piece.getTeamColor() != playerColor) {
                System.out.println("You can only move your own pieces.");
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
        if (currentGame == null) {
            System.out.println("No game loaded.");
            return;
        }

        System.out.print("Enter the position of the piece to highlight (e.g., 'e2'): ");
        String positionInput = scanner.nextLine().trim();

        ChessPosition position = parsePosition(positionInput);
        if (position == null) {
            System.out.println("Invalid position format. Use format like 'e2'.");
            return;
        }

        ChessPiece piece = currentGame.getBoard().getPiece(position);
        if (piece == null) {
            System.out.println("No piece at position " + positionInput);
            return;
        }

        // Get valid moves for this piece
        Collection<ChessMove> validMoves = currentGame.validMoves(position);

        if (validMoves.isEmpty()) {
            System.out.println("No legal moves available for piece at " + positionInput);
            drawBoard(currentGame.getBoard());
            return;
        }

        // Create set of positions to highlight
        Set<ChessPosition> highlightPositions = new HashSet<>();
        highlightPositions.add(position); // Highlight the piece's current position

        // Add all end positions of valid moves
        for (ChessMove move : validMoves) {
            highlightPositions.add(move.getEndPosition());
        }

        System.out.println("\nHighlighting legal moves for " + piece.getPieceType() +
                " at " + positionInput + ":");
        drawBoardWithHighlights(currentGame.getBoard(), highlightPositions);

        System.out.println("Found " + validMoves.size() + " legal moves.");
    }

    private void drawBoardWithHighlights(ChessBoard board, Set<ChessPosition> highlights) {
        System.out.println();

        if (playerColor == ChessGame.TeamColor.BLACK) {
            drawBoardFromBlackPerspectiveWithHighlights(board, highlights);
        } else {
            drawBoardFromWhitePerspectiveWithHighlights(board, highlights);
        }
    }

    private void drawBoardFromWhitePerspectiveWithHighlights(ChessBoard board, Set<ChessPosition> highlights) {
        System.out.println("   a  b  c  d  e  f  g  h");
        System.out.println("  ┌──┬──┬──┬──┬──┬──┬──┬──┐");

        for (int row = 8; row >= 1; row--) {
            System.out.print(row + " │");
            for (int col = 1; col <= 8; col++) {
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(pos);

                if (highlights.contains(pos)) {
                    // Highlight this position
                    System.out.print(EscapeSequences.SET_BG_COLOR_YELLOW +
                            EscapeSequences.SET_TEXT_COLOR_BLACK +
                            getPieceSymbol(piece) +
                            EscapeSequences.RESET_BG_COLOR +
                            EscapeSequences.RESET_TEXT_COLOR + " │");
                } else {
                    System.out.print(getPieceSymbol(piece) + " │");
                }
            }
            System.out.println(" " + row);

            if (row > 1) {
                System.out.println("  ├──┼──┼──┼──┼──┼──┼──┼──┤");
            }
        }

        System.out.println("  └──┴──┴──┴──┴──┴──┴──┴──┘");
        System.out.println("   a  b  c  d  e  f  g  h");
    }

    private void drawBoardFromBlackPerspectiveWithHighlights(ChessBoard board, Set<ChessPosition> highlights) {
        System.out.println("   h  g  f  e  d  c  b  a");
        System.out.println("  ┌──┬──┬──┬──┬──┬──┬──┬──┐");

        for (int row = 1; row <= 8; row++) {
            System.out.print(row + " │");
            for (int col = 8; col >= 1; col--) {
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(pos);

                if (highlights.contains(pos)) {
                    // Highlight this position
                    System.out.print(EscapeSequences.SET_BG_COLOR_YELLOW +
                            EscapeSequences.SET_TEXT_COLOR_BLACK +
                            getPieceSymbol(piece) +
                            EscapeSequences.RESET_BG_COLOR +
                            EscapeSequences.RESET_TEXT_COLOR + " │");
                } else {
                    System.out.print(getPieceSymbol(piece) + " │");
                }
            }
            System.out.println(" " + row);

            if (row < 8) {
                System.out.println("  ├──┼──┼──┼──┼──┼──┼──┼──┤");
            }
        }

        System.out.println("  └──┴──┴──┴──┴──┴──┴──┴──┘");
        System.out.println("   h  g  f  e  d  c  b  a");
    }

    private boolean isGameOver() {
        if (currentGame == null) {
            return false;
        }

        return currentGame.isInCheckmate(ChessGame.TeamColor.WHITE) ||
                currentGame.isInCheckmate(ChessGame.TeamColor.BLACK) ||
                currentGame.isInStalemate(ChessGame.TeamColor.WHITE) ||
                currentGame.isInStalemate(ChessGame.TeamColor.BLACK);
    }

    @Override
    public void notify(ServerMessage message) {
        switch (message.getServerMessageType()) {
            case LOAD_GAME -> {
                LoadGameMessage loadMessage = (LoadGameMessage) message;
                currentGame = (ChessGame) loadMessage.getGame();
                System.out.println("\n" + EscapeSequences.ERASE_LINE + "Game board updated:");
                drawBoard(currentGame.getBoard());

                // Check if game is over and inform user
                if (isGameOver()) {
                    System.out.println(EscapeSequences.SET_TEXT_COLOR_RED +
                            "Game is over - no more moves allowed." +
                            EscapeSequences.RESET_TEXT_COLOR);
                }

                System.out.print(">>> ");
            }
            case ERROR -> {
                ErrorMessage errorMessage = (ErrorMessage) message;
                System.out.println("\n" + EscapeSequences.SET_TEXT_COLOR_RED +
                        "ERROR: " + errorMessage.getErrorMessage() +
                        EscapeSequences.RESET_TEXT_COLOR);
                System.out.print(">>> ");
            }
            case NOTIFICATION -> {
                NotificationMessage notification = (NotificationMessage) message;
                System.out.println("\n" + EscapeSequences.SET_TEXT_COLOR_BLUE +
                        notification.getMessage() +
                        EscapeSequences.RESET_TEXT_COLOR);
                System.out.print(">>> ");
            }
        }
    }
}