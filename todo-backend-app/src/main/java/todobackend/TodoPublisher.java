package todobackend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.quarkus.logging.Log;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TodoPublisher {

    static final String SUBJECT = "todos";

    @ConfigProperty(name = "nats.url")
    String natsUrl;

    @Inject
    ObjectMapper mapper;

    private volatile Connection connection;

    void connect(@Observes StartupEvent event) throws InterruptedException {
        Options options = new Options.Builder()
                .server(natsUrl)
                .maxReconnects(-1)
                .connectionListener((conn, type) -> {
                    Log.info("NATS " + type);
                    if (type == ConnectionListener.Events.CONNECTED) {
                        connection = conn;
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

    void publish(@Observes(during = TransactionPhase.AFTER_SUCCESS) TodoEvent event) {
        if (connection == null) {
            Log.warn("Not connected to NATS, todo " + event.id() + " is not broadcast");
            return;
        }
        try {
            connection.publish(SUBJECT, mapper.writeValueAsBytes(event));
            Log.info("Published " + event.action() + " of todo " + event.id() + " to " + SUBJECT);
        } catch (JsonProcessingException e) {
            Log.error("Could not publish todo " + event.id(), e);
        }
    }
}
