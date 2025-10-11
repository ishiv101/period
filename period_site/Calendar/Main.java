import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

public class Main {
    public static void main(String[] args) throws IOException {
        int desired = resolvePort(args);
        HttpServer server = bindServerWithFallback(desired, 20); // try desired..desired+19, then port 0
        final int actualPort = server.getAddress().getPort();

        // tiny frontend at "/"
        server.createContext("/", (HttpExchange ex) -> {
            if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) { sendCorsPreflight(ex); return; }
            String html = """
            <!doctype html>
            <html lang="en">
            <meta charset="utf-8"/>
            <meta name="viewport" content="width=device-width,initial-scale=1"/>
            <title>Period · Demo</title>
            <body style="font-family:system-ui,Segoe UI,Arial;max-width:680px;margin:40px auto;padding:0 16px;">
              <h1>Cycle Tracker Demo</h1>
              <label>Last period date:
                <input id="last" type="date"/>
              </label>
              <button id="go">Check</button>
              <pre id="out" style="background:#f6f6f6;padding:12px;border-radius:8px;"></pre>

              <script>
                const out = document.getElementById('out');
                document.getElementById('go').onclick = async () => {
                  const last = document.getElementById('last').value || new Date().toISOString().slice(0,10);
                  try {
                    // Same-origin fetch to this server:
                    const res = await fetch('/cycle?last=' + encodeURIComponent(last));
                    const data = await res.json();
                    out.textContent = JSON.stringify(data, null, 2);
                  } catch (e) {
                    out.textContent = 'Error: ' + e;
                  }
                };
              </script>
            </body>
            </html>
            """;
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
        });

        // JSON API at "/cycle?last=YYYY-MM-DD"
        server.createContext("/cycle", (HttpExchange ex) -> {
            if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) { sendCorsPreflight(ex); return; }
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
            Map<String,String> q = parseQuery(ex.getRequestURI().getRawQuery());
            String last = q.getOrDefault("last", LocalDate.now().minusDays(10).toString());
            CalendarLogic tracker = new CalendarLogic(LocalDate.parse(last));

            String json = String.format(
                "{ \"last\":\"%s\", \"day\": %d, \"nextPeriod\": \"%s\", \"phase\": \"%s\" }",
                tracker.getLastPeriod(), tracker.dayOfCycle(), tracker.nextPeriodDate(), tracker.cyclePhase()
            );

            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
        });

        // optional quick health check
        server.createContext("/health", ex -> {
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.sendResponseHeaders(200, 0);
            ex.getResponseBody().close();
        });

        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nStopping server...");
            server.stop(0);
            System.out.println("Server stopped.");
        }));

        server.start();
        System.out.println("Open: http://localhost:" + actualPort + "/");
    }

    private static int resolvePort(String[] args) {
        // priority: env PORT -> CLI arg -> 8000
        String env = System.getenv("PORT");
        if (env != null && env.matches("\\d+")) return Integer.parseInt(env);
        if (args != null && args.length > 0 && args[0].matches("\\d+")) return Integer.parseInt(args[0]);
        return 8000;
    }

    private static HttpServer bindServerWithFallback(int startPort, int tries) throws IOException {
        int port = startPort;
        for (int i = 0; i < tries; i++) {
            try {
                return HttpServer.create(new InetSocketAddress(port), 0);
            } catch (IOException ioe) {
                // If it's a bind issue, try next port; otherwise rethrow
                if (isBindException(ioe)) {
                    port++;
                    continue;
                }
                throw ioe;
            }
        }
        // final fallback: let OS assign a free port
        return HttpServer.create(new InetSocketAddress(0), 0);
    }

    private static boolean isBindException(IOException ioe) {
        if (ioe instanceof BindException) return true;
        Throwable c = ioe.getCause();
        while (c != null) {
            if (c instanceof BindException) return true;
            c = c.getCause();
        }
        return (ioe.getMessage() != null && ioe.getMessage().toLowerCase().contains("address already in use"));
    }

    private static void sendCorsPreflight(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        ex.sendResponseHeaders(204, -1);
        ex.close();
    }

    private static Map<String,String> parseQuery(String raw) {
        if (raw == null || raw.isEmpty()) return Map.of();
        return java.util.Arrays.stream(raw.split("&"))
            .map(s -> s.split("=", 2))
            .collect(Collectors.toMap(a -> decode(a[0]), a -> a.length>1? decode(a[1]) : ""));
    }

    private static String decode(String s) {
        return java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
