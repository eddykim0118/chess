package server;

import com.google.gson.Gson;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;

import service.ClearService;
import service.ServiceException;
import service.UserService;

import spark.Request;
import spark.Response;
import spark.Spark;

public class Server {
    private final DataAccess dataAccess;
    private final ClearService clearService;
    private final UserService userService;
    private final Gson gson;

    public Server() {
        this.dataAccess = new MemoryDataAccess();
        this.clearService = new ClearService(dataAccess);
        this.userService = new UserService(dataAccess);
        this.gson = new Gson();
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);

        Spark.staticFiles.location("web");

        Spark.delete("/db", this::clearHandler);
        Spark.post("/user", this::registerHandler);

//        Spark.exception(Exception.class, this::exceptionHandler);

        Spark.notFound((req, res) -> {
            res.type("application/json");
            res.status(404);
            return gson.toJson(new ErrorResponse("Error: Endpoint not found"));
        });

        Spark.exception(Exception.class, (exception, request, response) -> {
            response.status(500);
            response.type("application/json");
            response.body(gson.toJson(new ErrorResponse("Error: " + exception.getMessage())));
        });

        //This line initializes the server and can be removed once you have a functioning endpoint
        Spark.init();

        Spark.awaitInitialization();
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
//        Spark.awaitStop();
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
            UserService.RegisterRequest registerRequest = gson.fromJson(req.body(), UserService.RegisterRequest.class);
            UserService.RegisterResult result = userService.register(registerRequest);

            res.status(200);
            res.type("application/json");
            return gson.toJson(result);

        } catch (ServiceException e) {
            res.type("application/json");

            if (e.getMessage().equals("Bad request")) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Error: bad request"));
            } else if (e.getMessage().equals("Already taken")) {
                res.status(403);
                return gson.toJson(new ErrorResponse("Error: already taken"));
            } else {
                res.status(500);
                return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
            }
        }
    }
    private record ErrorResponse(String message) {}

//    private void exceptionHandler(Exception e, Request req, Response res) {
//        System.err.println("Unhandled exception in endpoint " + req.pathInfo() + ": " + e.getMessage());
//
//        res.status(500);
//        res.type("application/json");
//
//        String errorMessage = "Error: " + (e.getMessage() != null ? e.getMessage() : "Internal server error");
//
//        res.body(gson.toJson(new ErrorResponse(errorMessage)));
//    }

//    private record ErrorResponse(String message) {}
//
//    public static void main(String[] args) {
//        Server server = new Server();
//        int port = server.run(8080);
//        System.out.println("Server started on port: " + port);
//        System.out.println("Test: curl -X DELETE http://localhost:" + port + "/db");
//        System.out.println("Press Ctrl+C to stop server");
//
//        // Keep server running until interrupted
//        try {
//            Thread.sleep(Long.MAX_VALUE);
//        } catch (InterruptedException e) {
//            System.out.println("Server stopping...");
//            server.stop();
//        }
//    }
}