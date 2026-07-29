import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class App {
 
    public static void main(String[] args) throws Exception {
        Path file = Path.of("/usr/src/app/files/log.txt");
        Path filePingPong = Path.of("/usr/src/app/files/pingpong.txt");
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> {
            List<String> logLines = Files.exists(file) ? Files.readAllLines(file) : List.of();
            String uuidFromLog = logLines.size() > 0 ? logLines.get(logLines.size() - 1) : "No log entries yet";
            String pingPongCount = Files.exists(filePingPong) ? Files.readString(filePingPong) : "0";
            String response = uuidFromLog + "\nPing / Pongs: " + pingPongCount;
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        server.start();
    }
}
