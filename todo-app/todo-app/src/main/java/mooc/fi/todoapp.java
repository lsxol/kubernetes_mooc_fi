package mooc.fi;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.time.Instant;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("")
public class todoapp {

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST";
    }

    @GET
    @Path("/image")
    @Produces("image/jpg")
    public byte[] getImage() {
        java.nio.file.Path file = java.nio.file.Path.of("/usr/src/app/files/image.jpg");
        try {
            boolean fresh = Files.exists(file) && Files.getLastModifiedTime(file, LinkOption.NOFOLLOW_LINKS).toInstant()
                    .isAfter(Instant.now().minusSeconds(600));
            if (!fresh) {
                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(java.net.URI.create("https://picsum.photos/1200"))
                        .build();
                byte[] imageBytes = client.send(request, HttpResponse.BodyHandlers.ofByteArray()).body();
                Files.write(file, imageBytes);
                return imageBytes;
            } else {
                return Files.readAllBytes(file);
            }
        } catch (java.io.IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }
}