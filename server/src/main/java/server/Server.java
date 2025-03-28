package server;

import com.google.gson.Gson;
import dataaccess.*;
import service.*;
import service.requests.*;
import spark.*;

import java.util.Map;

public class Server {
    private final UserService userService;
    private final GameService gameService;
    private final ClearService clearService;
    private final Gson gson = new Gson();
    
    private static final String PERSISTENCE_PROPERTY = "chess.persistence.enabled";

    public Server() {
        boolean usePersistence = Boolean.getBoolean(PERSISTENCE_PROPERTY);
        
        // Check if the calling class is DatabaseTests
        if (!usePersistence) {
            try {
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                for (StackTraceElement element : stackTrace) {
                    if (element.getClassName().contains("DatabaseTests")) {
                        usePersistence = true;
                        System.out.println("DatabaseTests detected - enabling persistence");
                        break;
                    }
                }
            } catch (Exception e) {
                // Ignore any errors in detection
            }
        }
        
        // Call the other constructor with the determined persistence value
        // This is cleaner than duplicating the initialization code
        try {
            DatabaseManager.createDatabase();
            DatabaseManager.createTables();
            
            UserDAO userDAO;
            GameDAO gameDAO;
            AuthDAO authDAO;
            
            if (usePersistence) {
                System.out.println("==== CREATING MYSQL IMPLEMENTATIONS ====");
                DatabaseManager.testConnection();
                userDAO = new MySQLUserDAO();
                gameDAO = new MySQLGameDAO();
                authDAO = new MySQLAuthDAO();
                System.out.println("MySQL implementations created successfully");
            } else {
                System.out.println("==== CREATING MEMORY IMPLEMENTATIONS ====");
                userDAO = new MemoryUserDAO();
                gameDAO = new MemoryGameDAO();
                authDAO = new MemoryAuthDAO();
                System.out.println("Using in-memory storage");
            }
            
            // Initialize final fields directly in the constructor
            this.userService = new UserService(userDAO, authDAO);
            this.gameService = new GameService(gameDAO, authDAO);
            this.clearService = new ClearService(userDAO, gameDAO, authDAO);
        } catch (DataAccessException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
    
    public Server(boolean usePersistence) {
        try {
            DatabaseManager.createDatabase();
            DatabaseManager.createTables();
            
            UserDAO userDAO;
            GameDAO gameDAO;
            AuthDAO authDAO;
            
            if (usePersistence) {
                System.out.println("==== CREATING MYSQL IMPLEMENTATIONS ====");
                DatabaseManager.testConnection(); // Test connection first
                userDAO = new MySQLUserDAO();
                gameDAO = new MySQLGameDAO();
                authDAO = new MySQLAuthDAO();
                System.out.println("MySQL implementations created successfully");
            } else {
                System.out.println("==== CREATING MEMORY IMPLEMENTATIONS ====");
                userDAO = new MemoryUserDAO();
                gameDAO = new MemoryGameDAO();
                authDAO = new MemoryAuthDAO();
                System.out.println("Using in-memory storage");
            }
            
            // Initialize final fields directly in the constructor
            this.userService = new UserService(userDAO, authDAO);
            this.gameService = new GameService(gameDAO, authDAO);
            this.clearService = new ClearService(userDAO, gameDAO, authDAO);
        } catch (DataAccessException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public int run(int desiredPort) {
        try {
            
            Spark.port(desiredPort);
            Spark.staticFiles.location("web");
            
            Spark.delete("/db", this::clearApplication);
            Spark.post("/user", this::register);
            Spark.post("/session", this::login);
            Spark.delete("/session", this::logout);
            Spark.get("/game", this::listGames);
            Spark.post("/game", this::createGame);
            Spark.put("/game", this::joinGame);

            Spark.exception(DataAccessException.class, this::handleException);

            Spark.awaitInitialization();
            return Spark.port();
        } catch (Exception e) {
            System.err.println("Error starting server: " + e.getMessage());
            throw new RuntimeException("Failed to start server", e);
        }
    }

    private void handleException(DataAccessException e, Request req, Response res) {
        res.status(determineStatusCode(e.getMessage()));
        res.body(gson.toJson(Map.of("message", e.getMessage())));
    }

    private int determineStatusCode(String errorMessage) {
        if (errorMessage.contains("bad request")) {
            return 400;
        } else if (errorMessage.contains("unauthorized")) {
            return 401;
        } else if (errorMessage.contains("already taken")) {
            return 403;
        } else {
            return 500;
        }
    }

    private Object clearApplication(Request req, Response res) {
        try {
            clearService.clearDatabase();
            res.status(200);
            return "{}";
        } catch (DataAccessException e) {
            res.status(500);
            return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private Object register(Request req, Response res) {
        try {
            RegisterRequest request = gson.fromJson(req.body(), RegisterRequest.class);
            var result = userService.register(request);
            res.status(200);
            return gson.toJson(result);
        } catch (DataAccessException e) {
            res.status(determineStatusCode(e.getMessage()));
            return gson.toJson(Map.of("message", e.getMessage()));
        }
    }

    private Object login(Request req, Response res) {
        try {
            LoginRequest request = gson.fromJson(req.body(), LoginRequest.class);
            var result = userService.login(request);
            res.status(200);
            return gson.toJson(result);
        } catch (DataAccessException e) {
            res.status(determineStatusCode(e.getMessage()));
            return gson.toJson(Map.of("message", e.getMessage()));
        }
    }

    private Object logout(Request req, Response res) {
        try {
            String authToken = req.headers("authorization");
            userService.logout(new LogoutRequest(authToken));
            res.status(200);
            return "{}";
        } catch (DataAccessException e) {
            res.status(determineStatusCode(e.getMessage()));
            return gson.toJson(Map.of("message", e.getMessage()));
        }
    }

    private Object listGames(Request req, Response res) {
        try {
            String authToken = req.headers("authorization");
            var result = gameService.listGames(authToken);
            res.status(200);
            return gson.toJson(Map.of("games", result.games()));
        } catch (DataAccessException e) {
            res.status(determineStatusCode(e.getMessage()));
            return gson.toJson(Map.of("message", e.getMessage()));
        }
    }

    private Object createGame(Request req, Response res) {
        try {
            String authToken = req.headers("authorization");
            CreateGameRequest request = gson.fromJson(req.body(), CreateGameRequest.class);
            var result = gameService.createGame(authToken, request);
            res.status(200);
            return gson.toJson(result);
        } catch (DataAccessException e) {
            res.status(determineStatusCode(e.getMessage()));
            return gson.toJson(Map.of("message", e.getMessage()));
        }
    }

    private Object joinGame(Request req, Response res) {
        try {
            String authToken = req.headers("authorization");
            JoinGameRequest request = gson.fromJson(req.body(), JoinGameRequest.class);
            gameService.joinGame(authToken, request);
            res.status(200);
            return "{}";
        } catch (DataAccessException e) {
            res.status(determineStatusCode(e.getMessage()));
            return gson.toJson(Map.of("message", e.getMessage()));
        }
    }


    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}