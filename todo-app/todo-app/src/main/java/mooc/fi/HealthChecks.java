package mooc.fi;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class HealthChecks {

    @Inject
    AppState state;

    @ConfigProperty(name = "todo.backend.url")
    String backendUrl;

    private final HttpClient client = HttpClient.newHttpClient();

    @Produces
    @Liveness
    HealthCheck app() {
        return () -> state.isHealthy() ? HealthCheckResponse.up("app") : HealthCheckResponse.down("app");
    }

    @Produces
    @Readiness
    HealthCheck backend() {
        return () -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(backendUrl + "/healthz/ready"))
                        .timeout(Duration.ofSeconds(2))
                        .build();
                int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
                return status == 200 ? HealthCheckResponse.up("backend") : HealthCheckResponse.down("backend");
            } catch (Exception e) {
                return HealthCheckResponse.down("backend");
            }
        };
    }
}
