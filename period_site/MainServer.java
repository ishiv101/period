import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;

public class MainServer {
    public static void main(String[] args) throws Exception {
        int port = 8080;

        // Check for required system property (your API key)
        if (System.getProperty("GEMINI_API") == null || System.getProperty("GEMINI_API").isEmpty()) {
            System.err.println("FATAL: You must start with: java -DGEMINI_API=your_api_key MainServer");
            return;
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(10));

        System.out.println("🌸 LunaCycle running on http://localhost:" + port);

        // Serve static files
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";

            File file = new File("public" + path);
            if (file.exists() && !file.isDirectory()) {
                String contentType = getContentType(path);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.getResponseHeaders().set("Cache-Control", "no-cache");

                byte[] bytes = Files.readAllBytes(file.toPath());
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } else {
                sendErrorResponse(exchange, 404, "Not Found");
            }
            exchange.close();
        });

        // ✅ Chatbot API endpoint
        server.createContext("/api/chat", exchange -> {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    String body = new String(exchange.getRequestBody().readAllBytes());
                    String responseJson = processChatRequest(body);

                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    byte[] bytes = responseJson.getBytes("UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                } catch (Exception e) {
                    e.printStackTrace();
                    sendErrorResponse(exchange, 500, "ChatBot Error: " + e.getMessage());
                }
            } else {
                sendErrorResponse(exchange, 405, "Method Not Allowed");
            }
            exchange.close();
        });

        // ✅ New: Cycle tracking endpoint
        server.createContext("/cycle", new CycleHandler());

        server.start();
    }

    // ===================== NEW HANDLER =====================
    static class CycleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

            String query = exchange.getRequestURI().getQuery();
            if (query == null || !query.startsWith("last=")) {
                sendErrorResponse(exchange, 400, "Missing ?last=YYYY-MM-DD");
                return;
            }

            String lastDate = query.substring(5);
            try {
                LocalDate start = LocalDate.parse(lastDate);
                LocalDate today = LocalDate.now();
                long day = ChronoUnit.DAYS.between(start, today) + 1;
                if (day < 1) day = 1;
                long cycleLength = 28;
                LocalDate nextPeriod = start.plusDays(cycleLength);

                // Determine cycle phase
                String phase;
                if (day <= 5) phase = "Menstrual";
                else if (day <= 13) phase = "Follicular";
                else if (day <= 17) phase = "Ovulation";
                else if (day <= 28) phase = "Luteal";
                else phase = "Menstrual";

                String symptoms = switch (phase) {
                    case "Menstrual" -> "Cramps, fatigue, mood changes";
                    case "Follicular" -> "Rising energy, clear skin, improved mood";
                    case "Ovulation" -> "Bloating, high libido, light cramps";
                    case "Luteal" -> "Tender breasts, irritability, food cravings";
                    default -> "Mild changes";
                };

                String json = String.format(
                    "{\"last\":\"%s\",\"day\":%d,\"phase\":\"%s\",\"nextPeriod\":\"%s\",\"symptoms\":\"%s\"}",
                    lastDate, day, phase, nextPeriod, symptoms
                );

                byte[] bytes = json.getBytes("UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (Exception e) {
                sendErrorResponse(exchange, 400, "Invalid date format. Use YYYY-MM-DD");
            } finally {
                exchange.close();
            }
        }
    }

    // ===================== EXISTING HELPERS =====================
    private static void sendErrorResponse(HttpExchange exchange, int status, String message) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        byte[] msg = message.getBytes();
        exchange.sendResponseHeaders(status, msg.length);
        exchange.getResponseBody().write(msg);
        exchange.close();
    }

    private static String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        return "text/plain";
    }

    public static String processChatRequest(String requestBody) {
        // Crude JSON parsing
        String userMessage = extractValueStatic(requestBody, "message");
        String symptoms = extractValueStatic(requestBody, "symptoms");
        String cycleDay = extractValueStatic(requestBody, "cycleDay");

        // Call your Gemini API logic
        ChatBot bot = new ChatBot(); // create instance
        String aiResponse = bot.callGeminiApi(userMessage, symptoms, cycleDay);

        // Return JSON for frontend
        return "{\"response\": \"" + escapeJsonStatic(aiResponse) + "\"}";
    }

    private static String extractValueStatic(String json, String key) {
        String search = "\"" + key + "\":\"";
        int startIndex = json.indexOf(search);
        if (startIndex == -1) return "";
        startIndex += search.length();
        int endIndex = json.indexOf("\"", startIndex);
        if (endIndex == -1) return "";
        return json.substring(startIndex, endIndex).trim();
    }

    private static String escapeJsonStatic(String s) {
        return s.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
