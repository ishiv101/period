// handles Gemini API calls
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ChatBot {

    private static final String API_KEY = System.getProperty("GEMINI_API");
    private static final String MODEL_NAME = "gemini-2.5-flash-preview-05-20";

    public static String processChatRequest(String requestBody) {
        // Crude JSON parsing
        String userMessage = extractValue(requestBody, "message");
        String symptoms = extractValue(requestBody, "symptoms");
        String cycleDay = extractValue(requestBody, "cycleDay");

        String aiResponse = callGeminiApi(userMessage, symptoms, cycleDay);

        // Return JSON
        return "{\"response\": \"" + escapeJson(aiResponse) + "\"}";
    }

    public static String callGeminiApi(String userMessage, String symptoms, String cycleDay) {
        String fullPrompt = buildGeminiPrompt(userMessage, symptoms, cycleDay);
        try {
            String apiEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_NAME + ":generateContent?key=" + API_KEY;
            URL url = new URL(apiEndpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String systemInstruction = "You are a friendly, compassionate, and non-diagnostic AI specialist focusing on menstrual cycle health. Include the user's phase and 2 practical tips. Be concise and kind.";

            String jsonPayload = String.format(
                "{\"contents\": [{\"parts\": [{\"text\": \"%s\"}]}], \"systemInstruction\": {\"parts\": [{\"text\": \"%s\"}]}}",
                escapeJson(fullPrompt),
                escapeJson(systemInstruction)
            );

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8
            ));
            String responseText = br.lines().reduce("", (a, b) -> a + b);

            if (responseCode >= 400) {
                return "Gemini API Error (" + responseCode + "): " + responseText;
            }

            // Extract the AI text
            Pattern pattern = Pattern.compile("\"text\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(responseText);
            StringBuilder extracted = new StringBuilder();
            while (matcher.find()) {
                extracted.append(matcher.group(1)).append("\n");
            }
            String extractedText = extracted.toString().trim();
            return extractedText.isEmpty() ? "AI returned an empty response." : extractedText;

        } catch (IOException e) {
            return "Network Error: Cannot connect to the Gemini API.";
        }
    }

    private static String buildGeminiPrompt(String userMessage, String symptoms, String cycleDay) {
        return String.format(
            "User Context:\n- Current Cycle Day: %s\n- Reported Symptoms: %s\nUser's Question: %s",
            cycleDay, symptoms, userMessage
        );
    }

    private static String extractValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int startIndex = json.indexOf(search);
        if (startIndex == -1) return "";
        startIndex += search.length();
        int endIndex = json.indexOf("\"", startIndex);
        if (endIndex == -1) return "";
        return json.substring(startIndex, endIndex).trim();
    }

    private static String escapeJson(String s) {
        return s.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
