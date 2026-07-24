import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.UUID;

public class App {
    public static void main(String[] args) throws Exception {
        String uuid = UUID.randomUUID().toString();
        
        new Thread(() -> {
            while(true){
                System.out.println(Instant.now() + " " + uuid);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> {
            String response = Instant.now() + ": " + uuid;
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        server.start();
    }
}
