package broadcaster;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class NatsReadiness implements HealthCheck {

    @Inject
    Broadcaster broadcaster;

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("nats").status(broadcaster.isConnected()).build();
    }
}
