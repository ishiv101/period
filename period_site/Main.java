import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;

public class Main{
    private static final String API_KEY = "AIzaSyDBPLQ8Zz3O6rcZUZV4tYFZrwSlnz6OXB4";
    
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(system.in);

        System.out.println("Welcome to Period -- your AI cycle companion!")
        System.out.print("Enter the start date of your last period (YYYY-MM-DD): ")
        String symptoms = scanner.nextLine();

        int cycleDay = getCycleDay(inputDate);
        System.out.println("\nYou are on day " + cycleDay + " of your cycle.\n")

        String prompt = "You are a friendly women's health assistant. " 
                    + "The user is on day " + cycleDay + " of their menstural cycle and reports: " 
                    + symptoms + ". Tell them whhat phase of their cycle they are in and give a brief explanation of what that means."
                    + "Give them 2 practical tips for how to alleviate symptoms" + "Be kind and concise.";
        
        String aiResponse = getGeminiResponse(prompt);
        System.out.println(aiResponse);
    }

    public static in getCycleDay(String lastPeriodDate){
        try(
            LocalDate lastDate = LocalDate.parse(lastPeriodDate, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate today = LocalDate.now();
            long daysBetween = ChronoUnit.DAYS.between(lastDate, today);
            return (int) (daysBetween % 28);
        ) catch (Exception e) {
            System.out.println("Invalid date format. Please use YYYY-MM-DD.");
            return 0;
        }
    }

    public static String getGeminiResponse(String prompt) throws Exception {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;

        String jsonBody = "{ \"contents\": [{ \"parts\": [{ \"text\": \"" + prompt.replace("\"", "\\\"") + "\" }] }] }";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Extract the text output (simple JSON parsing)
        String body = response.body();
        int start = body.indexOf("\"text\": \"") + 9;
        int end = body.indexOf("\"", start);
        if (start > 8 && end > start) {
            return body.substring(start, end).replace("\\n", "\n");
        } else {
            return "⚠️ Sorry, could not parse Gemini response:\n" + body;
        }
}