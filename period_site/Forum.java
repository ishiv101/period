import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// Represents a single post in the forum
class ForumPost {
    private static int nextId = 1;
    public final int id;
    public final String author;
    public final String content;
    public final String timestamp;

    public ForumPost(String author, String content) {
        this.id = nextId++;
        this.author = author;
        this.content = content;
        this.timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    // Returns the post as a JSON string for the client
    public String toJson() {
        return String.format(
            "{\"id\": %d, \"author\": \"%s\", \"content\": \"%s\", \"timestamp\": \"%s\"}",
            id,
            // Simple escaping for JSON compatibility
            escape(author),
            escape(content),
            timestamp
        );
    }
    
    // Simple helper for escaping quotes and newlines in strings
    private String escape(String s) {
        return s.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}

// Handles the in-memory forum logic
public class Forum {
    // Stores posts in memory (lost when server restarts)
    private static final List<ForumPost> postList = Collections.synchronizedList(new ArrayList<>());
    
    // Add initial dummy posts for testing
    static {
        postList.add(new ForumPost("AI Admin", "Welcome! Post your cycle questions and share experiences here."));
        postList.add(new ForumPost("Early Bird", "Day 8: Feeling great energy! Any workout tips for the follicular phase?"));
    }

    /**
     * Handles all HTTP requests for the /api/forum endpoint.
     * @param exchange The HTTP request/response context.
     */
    public static void handleForum(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            // Serve all posts
            String jsonArray = postList.stream()
                .map(ForumPost::toJson)
                .collect(Collectors.joining(",", "[", "]"));
            
            sendResponse(exchange, 200, jsonArray, "application/json");

        } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            // Add a new post
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            
            // Crude extraction of author and content from the incoming JSON
            String author = extractValue(requestBody, "author");
            String content = extractValue(requestBody, "content");

            if (author.isEmpty() || content.isEmpty()) {
                sendResponse(exchange, 400, "{\"error\": \"Missing author or content\"}", "application/json");
                return;
            }

            ForumPost newPost = new ForumPost(author, content);
            postList.add(newPost);

            sendResponse(exchange, 201, newPost.toJson(), "application/json");

        } else {
            sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
        }
    }
    
    // Helper to manually extract a value from a JSON string (CRUDE, for hackathon only)
    private static String extractValue(String json, String key) {
        String search = "\"" + key + "\": \"";
        int startIndex = json.indexOf(search);
        if (startIndex == -1) return "";
        
        startIndex += search.length();
        int endIndex = json.indexOf("\"", startIndex); 
        if (endIndex == -1) return "";
        
        String value = json.substring(startIndex, endIndex);
        return value.trim();
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }
}
