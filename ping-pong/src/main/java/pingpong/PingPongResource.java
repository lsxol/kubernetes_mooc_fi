package pingpong;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import pingpong.counter.Counter;

@Path("/pingpong")
public class PingPongResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String pingpong() {
        Counter counter = Counter.findById(1L);
        long current = counter.value;
        counter.value = current + 1;
        return "pong " + current;
    }

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String pingPongCount() {
        Counter counter = Counter.findById(1L);
        return String.valueOf(counter.value);
    }
}
