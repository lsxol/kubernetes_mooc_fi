package todobackend;

import io.quarkus.liquibase.LiquibaseFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import liquibase.Liquibase;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class DatabaseReadiness implements HealthCheck {

    @Inject
    LiquibaseFactory liquibaseFactory;

    private volatile boolean migrated = false;

    @Override
    public HealthCheckResponse call() {
        if (!migrated) {
            try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
                liquibase.update(liquibaseFactory.createContexts(), liquibaseFactory.createLabels());
                migrated = true;
            } catch (Exception e) {
                return HealthCheckResponse.down("database migration");
            }
        }
        return HealthCheckResponse.up("database migration");
    }
}
