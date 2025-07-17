package service;

import org.junit.jupiter.api.*;
import server.Server;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ManualClearEndpointTest {
    private static Server server;
    private static int port;

    @BeforeAll
    static void init() {
        server = new Server();
        port = server.run(0);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @Test
    @DisplayName("Clear Endpoint Returns 200 and Empty JSON")
    public void clearEndpointTest() throws Exception {
        URL url = new URL("http://localhost:" + port + "/db");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");

        int responseCode = connection.getResponseCode();
        Assertions.assertEquals(200, responseCode, "Clear endpoint should return 200");

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        Assertions.assertEquals("{}", response.toString().trim(), "Clear endpoint should return empty JSON object");
    }

    @Test
    @DisplayName("Server Returns 404 for Unknown Endpoint")
    public void unknownEndpointTest() throws Exception {
        URL url = new URL("http://localhost:" + port + "/unknown");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        Assertions.assertEquals(404, responseCode, "Unknown endpoint should return 404");
    }
}