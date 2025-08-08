import chess.*;
import server.Server;

public class Main {
    private static Server server;

    public static void main(String[] args) {
        server = new Server();
        var port = server.run(8080);
        System.out.println("♕ 240 Chess Server: " + port);
    }
}