import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class App {
 
    public static void main(String[] args) throws Exception {
        Path file = Path.of("/usr/src/app/files/log.txt");
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        HttpClient client = HttpClient.newHttpClient();
        Path infoFile = Path.of("/usr/src/app/config/information.txt");
        server.createContext("/", exchange -> {
            List<String> logLines = Files.exists(file) ? Files.readAllLines(file) : List.of();
            String fileContent = Files.exists(infoFile) ? Files.readString(infoFile).trim() : "No information file";
            String pingPongCount;
            try {
                HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(System.getenv("URI_PINGPONG") + "/count"))
                .build();
            pingPongCount = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            } catch (Exception e) {
                System.err.println("Error fetching ping-pong count: " + e.getMessage());
                pingPongCount = "0";
            }
            
            String uuidFromLog = logLines.size() > 0 ? logLines.get(logLines.size() - 1) : "No log entries yet";
            String response = "file content: " + fileContent
                + "\nenv variable: MESSAGE=" + System.getenv("MESSAGE")
                + "\n" + uuidFromLog
                + "\nPing / Pongs: " + pingPongCount;
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        server.start();
    }
}
