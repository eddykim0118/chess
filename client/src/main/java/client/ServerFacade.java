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
        var request = new RegisterRequest(username, password, email);
        var result = makeRequest("POST", "/user", request, RegisterResult.class, null);
        return new AuthData(result.authToken(), result.username());
    }

    public AuthData login(String username, String password) throws Exception {
        var request = new LoginRequest(username, password);
        var result = makeRequest("POST", "/session", request, LoginResult.class, null);
        return new AuthData(result.authToken(), result.username());
    }

    public void logout(String authToken) throws Exception {
        makeRequest("DELETE", "/session", null, null, authToken);
    }

    public GameData createGame(String authToken, String gameName) throws Exception {
        var request = new CreateGameRequest(gameName);
        var result = makeRequest("POST", "/game", request, CreateGameResult.class, authToken);
        return new GameData(result.gameID(), null, null, gameName, null);
    }

    public GameData[] listGames(String authToken) throws Exception {
        var result = makeRequest("GET", "/game", null, ListGamesResult.class, authToken);
        return result.games();
    }

    public void joinGame(String authToken, int gameId, String playerColor) throws Exception {
        var request = new JoinGameRequest(playerColor, gameId);
        makeRequest("PUT", "/game", request, null, authToken);
    }
    private <T> T makeRequest(String method, String path, Object request, Class<T> responseClass, String authToken) throws Exception {
        try {
            URL url = (new URI(serverUrl + path)).toURL();
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod(method);
            http.setDoOutput(true);

            if (authToken != null) {
                http.addRequestProperty("Authorization", authToken);
            }

            if (request != null) {
                http.addRequestProperty("Content-Type", "application/json");
                String reqData = gson.toJson(request);
                try (OutputStream reqBody = http.getOutputStream()) {
                    reqBody.write(reqData.getBytes());
                }
            }

            http.connect();
            throwIfNotSuccessful(http);

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

    public void clear() throws Exception {
        makeRequest("DELETE", "/db", null, null, null);
    }

    private record RegisterRequest(String username, String password, String email) {}
    private record RegisterResult(String username, String authToken) {}
    private record LoginRequest(String username, String password) {}
    private record LoginResult(String username, String authToken) {}

    private record CreateGameRequest(String gameName) {}
    private record CreateGameResult(int gameID) {}
    private record ListGamesResult(GameData[] games) {}
    private record JoinGameRequest(String playerColor, int gameID) {}

    private static void printColumnLabels(boolean reverse) {
        System.out.print("    ");
        if (reverse) {
            for (char col = 'h'; col >= 'a'; col--) {
                System.out.print(" " + col + "  ");
            }
        } else {
            for (char col = 'a'; col <= 'h'; col++) {
                System.out.print(" " + col + "  ");
            }
        }
        System.out.println();
    }
}