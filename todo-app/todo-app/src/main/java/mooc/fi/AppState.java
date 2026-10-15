package mooc.fi;

import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class AppState {

    private static final String FAILURE_PAGE = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>Todo APP</title>
                <style>
                    body { margin: 0; padding: 3rem 1rem; font-family: Arial, Helvetica, sans-serif; background-color: #fff3f3; }
                    .failure { max-width: 420px; margin: 0 auto; padding: 1.5rem 2rem; text-align: center; color: #8b1a1a;
                               background-color: #ffe3e3; border: 1px solid #e3a1a1; border-radius: 8px; }
                    .failure h1 { margin: 0 0 1rem; }
                </style>
            </head>
            <body>
                <div class="failure">
                    <h1>System Failure</h1>
                    <p>The Todo App is currently unhealthy. Please wait for recovery.</p>
                </div>
            </body>
            </html>
            """;

    private volatile boolean healthy = true;

    public boolean isHealthy() {
        return healthy;
    }

    public void breakApp() {
        healthy = false;
    }

    // Once broken, every request except the health endpoints gets the failure page,
    // until the liveness probe fails and Kubernetes restarts the container.
    void installBreakFilter(@Observes Router router) {
        router.route().order(-100).handler(ctx -> {
            if (healthy || ctx.normalizedPath().startsWith("/healthz")) {
                ctx.next();
                return;
            }
            ctx.response()
                    .setStatusCode(500)
                    .putHeader("Content-Type", "text/html")
                    .end(FAILURE_PAGE);
        });
    }
}
