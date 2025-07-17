package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.MemoryDataAccess;
import service.ClearService;
import spark.*;

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
            clearService.clear();
            res.status(200);
            res.type("application/json");
            return "{}";
        } catch (Exception e) {
            res.status(500);
            res.type("application/json");
            return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    private void exceptionHandler(Exception e, Request req, Response res) {
        res.status(500);
        res.type("application/json");
        res.body(gson.toJson(new ErrorResponse("Error: " + e.getMessage())));
    }

    private record ErrorResponse(String message) {}
}