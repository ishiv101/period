# main entry point to run web server

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MainServer {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        System.out.println("🌸 LunaCycle running on http://localhost:" + port);

        // Serve static files
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            File file = new File("public" + path);
            if (file.exists()) {
                exchange.sendResponseHeaders(200, file.length());
                OutputStream os = exchange.getResponseBody();
                Files.copy(file.toPath(), os);
                os.close();
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        });

        // Chatbot endpoint
        server.createContext("/api/chat", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes());
                String response = ChatBot.askGemini(body);
                byte[] bytes = response.getBytes();
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            }
        });

        // Forum endpoint
        server.createContext("/api/forum", Forum::handleForum);

        server.setExecutor(null);
        server.start();
    }
}
