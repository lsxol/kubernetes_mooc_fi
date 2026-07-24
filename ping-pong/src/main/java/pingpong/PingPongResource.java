package pingpong;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/pingpong")
public class PingPongResource {

    private final AtomicInteger counter = new AtomicInteger(0);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String pingPong() {
        return "pong " + counter.getAndIncrement();
    }
}
