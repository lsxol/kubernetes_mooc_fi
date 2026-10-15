package broadcaster;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.quarkus.logging.Log;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class Broadcaster {

    static final String SUBJECT = "todos";
    static final String QUEUE_GROUP = "broadcaster";

    @ConfigProperty(name = "nats.url")
    String natsUrl;

    @ConfigProperty(name = "webhook.url")
    String webhookUrl;

    @Inject
    ObjectMapper mapper;

    private final HttpClient client = HttpClient.newHttpClient();
    private volatile Connection connection;

    void connect(@Observes StartupEvent event) throws InterruptedException {
        Options options = new Options.Builder()
                .server(natsUrl)
                .maxReconnects(-1)
                .connectionListener((conn, type) -> {
                    Log.info("NATS " + type);
                    if (type == ConnectionListener.Events.CONNECTED) {
                        connection = conn;
                        conn.createDispatcher(this::forward).subscribe(SUBJECT, QUEUE_GROUP);
                        Log.info("Listening on " + SUBJECT + " in queue group " + QUEUE_GROUP);
                    }
                })
                .build();
        Nats.connectAsynchronously(options, true);
    }

    void close(@Observes ShutdownEvent event) throws InterruptedException {
        if (connection != null) {
            connection.close();
        }
    }

    public boolean isConnected() {
        return connection != null && connection.getStatus() == Connection.Status.CONNECTED;
    }

    void forward(Message message) {
        try {
            TodoEvent todo = mapper.readValue(message.getData(), TodoEvent.class);
            String text = todo.describe();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(new WebhookMessage("bot", text))))
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            Log.info("Sent \"" + text + "\", webhook answered " + response.statusCode());
        } catch (IOException e) {
            Log.error("Could not forward " + new String(message.getData()), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
