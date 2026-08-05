package pingpong;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.nio.file.Files;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/pingpong")
public class PingPongResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String pingPong() throws Exception {
        java.nio.file.Path file = java.nio.file.Path.of("/usr/src/app/files/pingpong.txt");
        Integer number = Files.exists(file) ? Integer.parseInt(Files.readString(file)) : 0;
        number++;
        Files.createDirectories(file.getParent());
        Files.writeString(file, number.toString());
        return "pong " + number.toString();
    }

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    public String pingPongCount() throws Exception {
        java.nio.file.Path file = java.nio.file.Path.of("/usr/src/app/files/pingpong.txt");
        Integer number = Files.exists(file) ? Integer.parseInt(Files.readString(file)) : 0;
        return number.toString();
    }
}
