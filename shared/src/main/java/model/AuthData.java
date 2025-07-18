package model;

public class AuthData {
    private final String authToken;
    private final String username;

    public AuthData(String authToken, String username) {
        this.authToken = authToken;
        this.username = username;
    }

    public String authToken() {
        return authToken;
    }

    public String username() {
        return username;
    }
}