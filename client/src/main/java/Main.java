import chess.*;
import client.ServerFacade;
import client.GameplayUI;
import model.AuthData;
import model.GameData;
import ui.EscapeSequences;
import java.util.Scanner;

public class Main {
    private static ServerFacade serverFacade;
    private static Scanner scanner;
    private static AuthData currentAuth = null;
    private static GameData[] lastGamesList = null;

    public static void main(String[] args) {
        System.out.println("♕ 240 Chess Client Starting...");

        serverFacade = new ServerFacade(8080);
        scanner = new Scanner(System.in);

        System.out.println("Welcome to Chess!");

        while (true) {
            if (currentAuth == null) {
                preloginUI();
            } else {
                postloginUI();
            }
        }
    }

    private static void preloginUI() {
        System.out.print("[LOGGED_OUT] >>> ");
        String input = scanner.nextLine().trim().toLowerCase();
        String[] tokens = input.split("\\s+");

        if (tokens.length == 0) {
            return;
        }

        String cmd = tokens[0];

        try {
            switch (cmd) {
                case "help" -> displayPreloginHelp();
                case "quit" -> {
                    System.out.println("Goodbye!");
                    System.exit(0);
                }
                case "login" -> login();
                case "register" -> register();
                default -> System.out.println("Unknown command. Type 'help' for available commands.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void postloginUI() {
        System.out.print("[" + currentAuth.getUsername() + "] >>> ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            return;
        }

        String[] tokens = input.toLowerCase().split("\\s+");
        String cmd = tokens[0];

        try {
            switch (cmd) {
                case "help" -> displayPostloginHelp();
                case "logout" -> logout();
                case "create" -> {
                    if (tokens.length < 2) {
                        System.out.println("Usage: create <gameName>");
                        return;
                    }
                    String gameName = input.substring(input.toLowerCase().indexOf("create") + 6).trim();
                    createGame(gameName);
                }
                case "list" -> listGames();
                case "play" -> {
                    if (tokens.length < 2) {
                        System.out.println("Usage: play <gameNumber> [WHITE|BLACK]");
                        return;
                    }
                    String gameNum = tokens[1];
                    String color = tokens.length > 2 ? tokens[2].toUpperCase() : null;
                    playGame(gameNum, color);
                }
                case "observe" -> {
                    if (tokens.length < 2) {
                        System.out.println("Usage: observe <gameNumber>");
                        return;
                    }
                    observeGame(tokens[1]);
                }
                default -> System.out.println("Unknown command. Type 'help' for available commands.");
            }
        } catch (Exception e) {
            System.out.println("An error occurred. Please try again.");
        }
    }

    private static void displayPreloginHelp() {
        System.out.println("Available commands:");
        System.out.println("  register - Create a new account");
        System.out.println("  login - Login to your account");
        System.out.println("  help - Display this help message");
        System.out.println("  quit - Exit the program");
    }

    private static void displayPostloginHelp() {
        System.out.println("Available commands:");
        System.out.println("  create <gameName> - Create a new game");
        System.out.println("  list - List all games");
        System.out.println("  play <gameNumber> [WHITE|BLACK] - Join a game as a player");
        System.out.println("  observe <gameNumber> - Observe a game");
        System.out.println("  logout - Logout and return to main menu");
        System.out.println("  help - Display this help message");
    }

    private static void login() throws Exception {
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("Username cannot be empty");
            return;
        }
        if (password.isEmpty()) {
            System.out.println("Password cannot be empty");
            return;
        }

        try {
            currentAuth = serverFacade.login(username, password);
            System.out.println("Login successful! Welcome " + currentAuth.getUsername());
        } catch (Exception e) {
            String cleanMessage = cleanErrorMessage(e.getMessage());
            System.out.println("Login failed: " + cleanMessage);
        }
    }

    private static void register() throws Exception {
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        System.out.print("Email: ");
        String email = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("Username cannot be empty");
            return;
        }
        if (password.isEmpty()) {
            System.out.println("Password cannot be empty");
            return;
        }
        if (email.isEmpty()) {
            System.out.println("Email cannot be empty");
            return;
        }
        if (!email.contains("@")) {
            System.out.println("Please enter a valid email address");
            return;
        }

        try {
            currentAuth = serverFacade.register(username, password, email);
            System.out.println("Registration successful! Welcome " + currentAuth.getUsername());
        } catch (Exception e) {
            String cleanMessage = cleanErrorMessage(e.getMessage());
            System.out.println("Registration failed: " + cleanMessage);
        }
    }

    private static void logout() throws Exception {
        try {
            serverFacade.logout(currentAuth.getAuthToken());
            System.out.println("Logged out successfully");
            currentAuth = null;
        } catch (Exception e) {
            System.out.println("Logout failed: " + e.getMessage());
            currentAuth = null;
        }
    }

    private static void createGame(String gameName) {
        if (gameName.isEmpty()) {
            System.out.println("Game name cannot be empty");
            return;
        }

        try {
            serverFacade.createGame(currentAuth.getAuthToken(), gameName);
            System.out.println("Game '" + gameName + "' created successfully");
        } catch (Exception e) {
            String cleanMessage = cleanErrorMessage(e.getMessage());
            System.out.println("Failed to create game: " + cleanMessage);
        }
    }

    private static void listGames() throws Exception {
        try {
            lastGamesList = serverFacade.listGames(currentAuth.getAuthToken());

            if (lastGamesList.length == 0) {
                System.out.println("No games found");
                return;
            }

            System.out.println("Games:");
            for (int i = 0; i < lastGamesList.length; i++) {
                GameData game = lastGamesList[i];
                String whitePlayer = game.getWhiteUsername() != null ? game.getWhiteUsername() : "empty";
                String blackPlayer = game.getBlackUsername() != null ? game.getBlackUsername() : "empty";

                System.out.printf("%d. %s - White: %s, Black: %s%n",
                        i + 1, game.getGameName(), whitePlayer, blackPlayer);
            }
        } catch (Exception e) {
            System.out.println("Failed to list games: " + e.getMessage());
        }
    }

    private static void playGame(String gameNumStr, String color) {
        if (color == null) {
            System.out.print("Player color (WHITE/BLACK): ");
            color = scanner.nextLine().trim().toUpperCase();
        }

        if (!color.equals("WHITE") && !color.equals("BLACK")) {
            System.out.println("Color must be WHITE or BLACK");
            return;
        }

        try {
            int gameNum = Integer.parseInt(gameNumStr);

            if (lastGamesList == null || gameNum < 1 || gameNum > lastGamesList.length) {
                System.out.println("Invalid game number. Use 'list' to see available games.");
                return;
            }

            GameData selectedGame = lastGamesList[gameNum - 1];
            serverFacade.joinGame(currentAuth.getAuthToken(), selectedGame.getGameID(), color);

            System.out.println("Successfully joined game '" + selectedGame.getGameName() + "' as " + color);

            ChessBoard board = selectedGame.getGame().getBoard();
            boolean whiteOnBottom = color.equals("WHITE");
            drawChessBoard(board, whiteOnBottom);

            ChessGame.TeamColor playerColor = color.equals("WHITE") ? ChessGame.TeamColor.WHITE : ChessGame.TeamColor.BLACK;
            GameplayUI gameplayUI = new GameplayUI(
                    scanner,
                    "http://localhost:8080",
                    currentAuth.getAuthToken(),
                    currentAuth.getUsername(),
                    selectedGame.getGameID(),
                    playerColor,
                    () -> {
                        System.out.println("Returned to main menu.");
                    }
            );

            gameplayUI.start();
        } catch (NumberFormatException e) {
            System.out.println("Invalid game number format. Please enter a number.");
        } catch (Exception e) {
            String cleanMessage = cleanErrorMessage(e.getMessage());
            System.out.println("Failed to join game: " + cleanMessage);
        }
    }

    private static void observeGame(String gameNumStr) {
        try {
            int gameNum = Integer.parseInt(gameNumStr);

            if (lastGamesList == null || gameNum < 1 || gameNum > lastGamesList.length) {
                System.out.println("Invalid game number. Use 'list' to see available games.");
                return;
            }

            GameData selectedGame = lastGamesList[gameNum - 1];

//          debug
            System.out.println("DEBUG: Attempting to observe game ID: " + selectedGame.getGameID());
            System.out.println("DEBUG: Auth token: " + (currentAuth.getAuthToken() != null ? "present" : "null"));

            serverFacade.joinGame(currentAuth.getAuthToken(), selectedGame.getGameID(), null);

            System.out.println("Now observing game '" + selectedGame.getGameName() + "'");

            ChessBoard board = selectedGame.getGame().getBoard();
            drawChessBoard(board, true);

            GameplayUI gameplayUI = new GameplayUI(
                    scanner,
                    "http://localhost:8080",
                    currentAuth.getAuthToken(),
                    currentAuth.getUsername(),
                    selectedGame.getGameID(),
                    null, // null means observer
                    () -> {
                        // Callback when gameplay exits - returns to main menu
                        System.out.println("Returned to main menu.");
                    }
            );
            gameplayUI.start();
        } catch (NumberFormatException e) {
            System.out.println("Invalid game number format. Please enter a number.");
        } catch (Exception e) {
            System.out.println("DEBUG: Full error message: " + e.getMessage());
            String cleanMessage = cleanErrorMessage(e.getMessage());
            System.out.println("Failed to observe game: " + cleanMessage);
        }
    }

    private static void drawChessBoard(ChessBoard board, boolean whiteOnBottom) {
        if (whiteOnBottom) {
            drawBoardWhitePerspective(board);
        } else {
            drawBoardBlackPerspective(board);
        }
    }

    private static void printColumnLabels(boolean reverse) {
        System.out.print("   ");
        if (reverse) {
            for (char col = 'h'; col >= 'a'; col--) {
                System.out.print(" " + col + " ");
            }
        } else {
            for (char col = 'a'; col <= 'h'; col++) {
                System.out.print(" " + col + " ");
            }
        }
        System.out.println();
    }

    private static void drawBoardWhitePerspective(ChessBoard board) {
        System.out.println();
        printColumnLabels(false);
        drawBoardRows(board, 8, 1, -1, 1, 8, 1);
        printColumnLabels(false);
        System.out.println();
    }

    private static void drawBoardBlackPerspective(ChessBoard board) {
        System.out.println();
        printColumnLabels(true);
        drawBoardRows(board, 1, 8, 1, 8, 1, -1);
        printColumnLabels(true);
        System.out.println();
    }

    private static void drawBoardRows(ChessBoard board, int startRow, int endRow, int rowIncrement,
                                      int startCol, int endCol, int colIncrement) {
        for (int row = startRow; (rowIncrement > 0 ? row <= endRow : row >= endRow); row += rowIncrement) {
            System.out.print(" " + row + " ");

            for (int col = startCol; (colIncrement > 0 ? col <= endCol : col >= endCol); col += colIncrement) {
                boolean isLightSquare = (row + col) % 2 == 1;
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);

                drawSquare(piece, isLightSquare);
            }

            System.out.println(" " + row);
        }
    }

    private static void drawSquare(ChessPiece piece, boolean isLightSquare) {
        String bgColor = isLightSquare ? EscapeSequences.SET_BG_COLOR_WHITE : EscapeSequences.SET_BG_COLOR_DARK_GREEN;
        String textColor = EscapeSequences.SET_TEXT_COLOR_BLACK;

        System.out.print(bgColor + textColor);

        if (piece == null) {
            System.out.print(EscapeSequences.EMPTY);
        } else {
            System.out.print(getPieceSymbol(piece));
        }

        System.out.print(EscapeSequences.RESET_BG_COLOR + EscapeSequences.RESET_TEXT_COLOR);
    }

    private static String getPieceSymbol(ChessPiece piece) {
        ChessPiece.PieceType type = piece.getPieceType();
        ChessGame.TeamColor color = piece.getTeamColor();

        if (color == ChessGame.TeamColor.WHITE) {
            return switch (type) {
                case KING -> EscapeSequences.WHITE_KING;
                case QUEEN -> EscapeSequences.WHITE_QUEEN;
                case BISHOP -> EscapeSequences.WHITE_BISHOP;
                case KNIGHT -> EscapeSequences.WHITE_KNIGHT;
                case ROOK -> EscapeSequences.WHITE_ROOK;
                case PAWN -> EscapeSequences.WHITE_PAWN;
            };
        } else {
            return switch (type) {
                case KING -> EscapeSequences.BLACK_KING;
                case QUEEN -> EscapeSequences.BLACK_QUEEN;
                case BISHOP -> EscapeSequences.BLACK_BISHOP;
                case KNIGHT -> EscapeSequences.BLACK_KNIGHT;
                case ROOK -> EscapeSequences.BLACK_ROOK;
                case PAWN -> EscapeSequences.BLACK_PAWN;
            };
        }
    }

    private static String cleanErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return "Unknown error occurred";
        }

        errorMessage = errorMessage.replaceAll("HTTP request failed with status: \\d+", "Request failed");
        errorMessage = errorMessage.replaceAll("HTTP request failed: ", "");

        if (errorMessage.contains("already taken")) {
            return "Username is already taken";
        }
        if (errorMessage.contains("unauthorized")) {
            return "Invalid credentials or session expired";
        }
        if (errorMessage.contains("bad request")) {
            return "Invalid input provided";
        }

        if (errorMessage.startsWith("Error: ")) {
            errorMessage = errorMessage.substring(7);
        }

        return errorMessage;
    }
}