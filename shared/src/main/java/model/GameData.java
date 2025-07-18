package model;

import chess.ChessGame;

public class GameData {
    private final int gameID;
    private final String whiteUsername;
    private final String blackUsername;
    private final String gameName;
    private final ChessGame game;

    public GameData(int gameID, String whiteUsername, String blackUsername, String gameName, ChessGame game) {
        this.gameID = gameID;
        this.whiteUsername = whiteUsername;
        this.blackUsername = blackUsername;
        this.gameName = gameName;
        this.game = game;
    }

    public int gameID() { return gameID; }
    public String whiteUsername() { return whiteUsername; }
    public String blackUsername() { return blackUsername; }
    public String gameName() { return gameName; }
    public ChessGame game() { return game; }
}