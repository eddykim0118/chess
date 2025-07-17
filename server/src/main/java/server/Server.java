package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import service.ClearService;
import spark.*;
import spark.Request;
import spark.Response;
import spark.Spark;

public class Server {
    private final DataAccess dataAccess;
    private final ClearService clearService;
    private final Gson gson;

    public Server() {
        this.dataAccess = new MemoryDataAccess();
        this.clearService = new ClearService(dataAccess);
        this.gson = new Gson();
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);

        Spark.staticFiles.location("web");

        // Register your endpoints and handle exceptions here.

        // Clear endpoint - Delete/db
        Spark.delete("/db", this::clearHandler);
        // Exception handling
        Spark.exception(Exception.class, this::exceptionHandler);
        // 404 Error Handling
        Spark.notFound((req, res) -> {
            res.type("application/json");
            res.status(404);
            return gson.toJson(new ErrorResponse("Error: Endpoint not found"));
        });

        //This line initializes the server and can be removed once you have a functioning endpoint
        Spark.init();

        Spark.awaitInitialization();
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }

    private Object clearHandler(Request req, Response res) {
        try {
            res.type("application/json");
            clearService.clear();
            res.status(200);
            return "{}";

        } catch (DataAccessException e) {
            res.status(500);
            return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
        } catch (Exception e) {
            res.status(500);
            res.type("application/json");
            return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    private void exceptionHandler(Exception e, Request req, Response res) {
        System.err.println("Unhandled exception in endpoint " + req.pathInfo() + ": " + e.getMessage());

        res.status(500);
        res.type("application/json");

        String errorMessage = "Error: " + (e.getMessage() != null ? e.getMessage() : "Internal server error");

        res.body(gson.toJson(new ErrorResponse(errorMessage)));
    }

    private record ErrorResponse(String message) {}

    public static void main(String[] args) {
        Server server = new Server();
        int port = server.run(8080);
        System.out.println("Server started on port: " + port);
        System.out.println("Test: curl -X DELETE http://localhost:" + port + "/db");
        System.out.println("Press Ctrl+C to stop server");

        // Keep server running until interrupted
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            System.out.println("Server stopping...");
            server.stop();
        }
    }
}