package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import service.ClearService;
import service.UserService;
import service.GameService;
import spark.Request;
import spark.Response;
import spark.Spark;

public class Server {
    private final DataAccess dataAccess;
    private final ClearService clearService;
    private final UserService userService;
    private final GameService gameService;
    private final Gson gson;

    public Server() {
        this.dataAccess = new MemoryDataAccess();
        this.clearService = new ClearService(dataAccess);
        this.userService = new UserService(dataAccess);
        this.gameService = new GameService(dataAccess);
        this.gson = new Gson();
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);
        Spark.staticFiles.location("web");

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

    private Object clearHandler(Request req, Response res) {
        try {
            clearService.clear();
            res.status(200);
            res.type("application/json");
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
            res.status(200);
            res.type("application/json");
            return gson.toJson(result);
        } catch (DataAccessException e) {
            res.type("application/json");
            if (e.getMessage().contains("bad request")) {
                res.status(400);
            } else if (e.getMessage().contains("already taken")) {
                res.status(403);
            } else {
                res.status(500);
            }
            return gson.toJson(new ErrorResponse(e.getMessage()));
        }
    }

    private Object loginHandler(Request req, Response res) {
        try {
            UserService.LoginRequest request = gson.fromJson(req.body(), UserService.LoginRequest.class);
            UserService.LoginResult result = userService.login(request);
            res.status(200);
            res.type("application/json");
            return gson.toJson(result);
        } catch (DataAccessException e) {
            res.type("application/json");
            if (e.getMessage().contains("bad request")) {
                res.status(400);
            } else if (e.getMessage().contains("unauthorized")) {
                res.status(401);
            } else {
                res.status(500);
            }
            return gson.toJson(new ErrorResponse(e.getMessage()));
        }
    }

    private Object logoutHandler(Request req, Response res) {
        try {
            String authToken = req.headers("authorization");
            userService.logout(authToken);
            res.status(200);
            res.type("application/json");
            return "{}";
        } catch (DataAccessException e) {
            res.type("application/json");
            if (e.getMessage().contains("unauthorized")) {
                res.status(401);
            } else {
                res.status(500);
            }
            return gson.toJson(new ErrorResponse(e.getMessage()));
        }
    }

    private Object listGamesHandler(Request req, Response res) {
        try {
            String authToken = req.headers("authorization");
            GameService.ListGamesResult result = gameService.listGames(authToken);
            res.status(200);
            res.type("application/json");
            return gson.toJson(result);
        } catch (DataAccessException e) {
            res.type("application/json");
            if (e.getMessage().contains("unauthorized")) {
                res.status(401);
            } else {
                res.status(500);
            }
            return gson.toJson(new ErrorResponse(e.getMessage()));
        }
    }

    private Object createGameHandler(Request req, Response res) {
        try {
            String authToken = req.headers("authorization");
            GameService.CreateGameRequest request = gson.fromJson(req.body(), GameService.CreateGameRequest.class);
            GameService.CreateGameResult result = gameService.createGame(request, authToken);
            res.status(200);
            res.type("application/json");
            return gson.toJson(result);
        } catch (DataAccessException e) {
            res.type("application/json");
            if (e.getMessage().contains("bad request")) {
                res.status(400);
            } else if (e.getMessage().contains("unauthorized")) {
                res.status(401);
            } else {
                res.status(500);
            }
            return gson.toJson(new ErrorResponse(e.getMessage()));
        }
    }

    private Object joinGameHandler(Request req, Response res) {
        try {
            String authToken = req.headers("authorization");
            GameService.JoinGameRequest request = gson.fromJson(req.body(), GameService.JoinGameRequest.class);
            gameService.joinGame(request, authToken);
            res.status(200);
            res.type("application/json");
            return "{}";
        } catch (DataAccessException e) {
            res.type("application/json");
            if (e.getMessage().contains("bad request")) {
                res.status(400);
            } else if (e.getMessage().contains("unauthorized")) {
                res.status(401);
            } else if (e.getMessage().contains("already taken")) {
                res.status(403);
            } else {
                res.status(500);
            }
            return gson.toJson(new ErrorResponse(e.getMessage()));
        }
    }

    private record ErrorResponse(String message) {}
}