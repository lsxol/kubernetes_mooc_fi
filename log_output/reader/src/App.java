import java.nio.file.Files;
import java.nio.file.Path;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class App {
 
    public static void main(String[] args) throws Exception {
        Path file = Path.of("/usr/src/app/files/log.txt");
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> {
            String response = Files.exists(file)
                ? Files.readString(file)
                : "No logs yet";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        server.start();
    }
}
