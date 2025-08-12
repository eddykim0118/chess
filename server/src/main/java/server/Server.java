package server;

import server.websocket.WebSocketHandler;
import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import dataaccess.MySqlDataAccess;
import service.ClearService;
import service.UserService;
import service.GameService;
import spark.Request;
import spark.Response;
import spark.Spark;
import static spark.Spark.webSocket;

public class Server {
    private final DataAccess dataAccess;
    private final ClearService clearService;
    private final UserService userService;
    private final GameService gameService;
    private final Gson gson;

    public Server() {
        try {
            this.dataAccess = new MySqlDataAccess();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
        this.clearService = new ClearService(dataAccess);
        this.userService = new UserService(dataAccess);
        this.gameService = new GameService(dataAccess);
        this.gson = new Gson();
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);
        Spark.staticFiles.location("web");

        webSocket("/ws", WebSocketHandler.class);
        WebSocketHandler.setDataAccess(dataAccess);

        Spark.delete("/db", this::clearHandler);
        Spark.post("/user", this::registerHandler);
        Spark.post("/session", this::loginHandler);
        Spark.delete("/session", this::logoutHandler);
        Spark.get("/game", this::listGamesHandler);
        Spark.post("/game", this::createGameHandler);
        Spark.put("/game", this::joinGameHandler);

        Spark.exception(Exception.class, (exception, request, response) -> {
            response.status(500);
            response.type("application/json");
            response.body(gson.toJson(new ErrorResponse("Error: " + exception.getMessage())));
        });

        Spark.awaitInitialization();
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
    }

    private void setJsonResponse(Response res, int status) {
        res.status(status);
        res.type("application/json");
    }

    private Object handleError(Response res, DataAccessException e) {
        res.type("application/json");

        String message = e.getMessage();
        if (message.contains("bad request")) {
            res.status(400);
        } else if (message.contains("unauthorized")) {
            res.status(401);
        } else if (message.contains("already taken")) {
            res.status(403);
        } else {
            res.status(500);
            // Ensure 500 errors always start with "Error:"
            if (!message.toLowerCase().contains("error")) {
                message = "Error: " + message;
            }
        }

        return gson.toJson(new ErrorResponse(message));
    }

    private Object clearHandler(Request req, Response res) {
        try {
            clearService.clear();
            setJsonResponse(res, 200);
            return "{}";
        } catch (DataAccessException e) {
            res.status(500);
            res.type("application/json");
            return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    private Object registerHandler(Request req, Response res) {
        try {
            UserService.RegisterRequest request = gson.fromJson(req.body(), UserService.RegisterRequest.class);
            UserService.RegisterResult result = userService.register(request);
            setJsonResponse(res, 200);
            return gson.toJson(result);
        } catch (DataAccessException e) {
            return handleError(res, e);
        }
    }

    private Object loginHandler(Request req, Response res) {
        try {
            UserService.LoginRequest request = gson.fromJson(req.body(), UserService.LoginRequest.class);
            UserService.LoginResult result = userService.login(request);
            setJsonResponse(res, 200);
            return gson.toJson(result);
        } catch (DataAccessException e) {
            return handleError(res, e);
        }
    }

    private Object logoutHandler(Request req, Response res) {
        try {
            String authToken = req.headers("authorization");
            userService.logout(authToken);
            setJsonResponse(res, 200);
            return "{}";
        } catch (DataAccessException e) {
            return handleError(res, e);
        }
    }

    private Object listGamesHandler(Request req, Response res) {
        try {
            String authToken = req.headers("authorization");
            GameService.ListGamesResult result = gameService.listGames(authToken);
            setJsonResponse(res, 200);
            return gson.toJson(result);
        } catch (DataAccessException e) {
            return handleError(res, e);
        }
    }

    private Object createGameHandler(Request req, Response res) {
        try {
            String authToken = req.headers("authorization");
            GameService.CreateGameRequest request = gson.fromJson(req.body(), GameService.CreateGameRequest.class);
            GameService.CreateGameResult result = gameService.createGame(request, authToken);
            setJsonResponse(res, 200);
            return gson.toJson(result);
        } catch (DataAccessException e) {
            return handleError(res, e);
        }
    }

    private Object joinGameHandler(Request req, Response res) {
        try {
            String authToken = req.headers("authorization");
            
            // Debug logging
            System.out.println("Raw request body: " + req.body());
            System.out.println("Auth token: " + authToken);
            
            GameService.JoinGameRequest request = gson.fromJson(req.body(), GameService.JoinGameRequest.class);
            
            System.out.println("Join game request: " + gson.toJson(request));
            
            gameService.joinGame(request, authToken);
            setJsonResponse(res, 200);
            return "{}";
        } catch (DataAccessException e) {
            System.out.println("DataAccessException caught: " + e.getMessage());
            return handleError(res, e);
        } catch (Exception e) {
            System.out.println("Unexpected exception caught: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            return handleError(res, new DataAccessException("Error: " + e.getMessage()));
        }
    }

    private record ErrorResponse(String message) {}
}