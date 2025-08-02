import chess.*;
import client.ServerFacade;
import model.AuthData;
import java.util.Scanner;

public class Main {
    private static ServerFacade serverFacade;
    private static Scanner scanner;
    private static AuthData currentAuth = null;

    public static void main(String[] args) {
//        var piece = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN);
//        System.out.println("♕ 240 Chess Client: " + piece);

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
        String input = scanner.nextLine().trim().toLowerCase();
        String[] tokens = input.split("\\s+");

        if (tokens.length == 0) {
            return;
        }

        String cmd = tokens[0];

        try {
            switch (cmd) {
                case "help" -> displayPostloginHelp();
                case "logout" -> logout();
                case "create" -> createGame();
                case "list" -> listGames();
                case "play" -> playGame();
                case "observe" -> observeGame();
                default -> System.out.println("Unknown command. Type 'help' for available commands.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
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

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Username and password cannot be empty");
            return;
        }

        try {
            currentAuth = serverFacade.login(username, password);
            System.out.println("Login successful! Welcome " + currentAuth.getUsername());
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }



    private static void register() throws Exception {
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        System.out.print("Email: ");
        String email = scanner.nextLine().trim();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            System.out.println("Username, password, and email cannot be empty");
            return;
        }

        try {
            currentAuth = serverFacade.register(username, password, email);
            System.out.println("Registration successful! Welcome " + currentAuth.getUsername());
        } catch (Exception e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }
}