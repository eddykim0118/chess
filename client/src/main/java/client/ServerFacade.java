package client;

import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import model.UserData;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class ServerFacade {
    private final String serverUrl;
    private final Gson gson = new Gson();

    public ServerFacade(int port) {
        this.serverUrl = "http://localhost:" + port;
    }

    public AuthData register(String username, String password, String email) throws Exception {
        var request = new UserData(username, password, email);
        return makeRequest("POST", "/user", request, AuthData.class, null);
    }

    public AuthData login(String username, String password) throws Exception {
        var request = new UserData(username, password, null);
        return makeRequest("POST", "/session", request, AuthData.class, null);
    }

    public void logout(String authToken) throws Exception {
        makeRequest("DELETE", "/session", null, null, authToken);
    }

    // Game management methods
    public GameData createGame(String authToken, String gameName) throws Exception {
        // Implementation will come in next commit
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public GameData[] listGames(String authToken) throws Exception {
        // Implementation will come in next commit
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void joinGame(String authToken, int gameId, String playerColor) throws Exception {
        // Implementation will come in next commit
        throw new UnsupportedOperationException("Not implemented yet");
    }

    // Helper method for making HTTP requests
    private <T> T makeRequest(String method, String path, Object request, Class<T> responseClass, String authToken) throws Exception {
        try {
            URL url = (new URI(serverUrl + path)).toURL();
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod(method);
            http.setDoOutput(true);

            // Add auth header if provided
            if (authToken != null) {
                http.addRequestProperty("Authorization", authToken);
            }

            // Add request body if provided
            if (request != null) {
                http.addRequestProperty("Content-Type", "application/json");
                String reqData = gson.toJson(request);
                try (OutputStream reqBody = http.getOutputStream()) {
                    reqBody.write(reqData.getBytes());
                }
            }

            http.connect();
            throwIfNotSuccessful(http);

            // Read response
            if (responseClass != null) {
                try (InputStream respBody = http.getInputStream()) {
                    InputStreamReader reader = new InputStreamReader(respBody);
                    return gson.fromJson(reader, responseClass);
                }
            }
            return null;

        } catch (Exception ex) {
            throw new Exception("HTTP request failed: " + ex.getMessage());
        }
    }

    private void throwIfNotSuccessful(HttpURLConnection http) throws IOException, Exception {
        var status = http.getResponseCode();
        if (!isSuccessful(status)) {
            throw new Exception("HTTP request failed with status: " + status);
        }
    }

    private static boolean isSuccessful(int status) {
        return status / 100 == 2;
    }
}