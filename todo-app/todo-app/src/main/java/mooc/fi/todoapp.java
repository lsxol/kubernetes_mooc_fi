package mooc.fi;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.time.Instant;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("")
public class todoapp {

    @ConfigProperty(name = "todo.backend.url", defaultValue = "http://todo-backend-app-service:2346")
    String backendUrl;

    private final HttpClient client = HttpClient.newHttpClient();

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST";
    }

    @GET
    @Path("/todos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTodos() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(backendUrl + "/todos"))
                .GET()
                .build();
        return forward(request, HttpResponse.BodyHandlers.ofString());
    }

    @POST
    @Path("/todos")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response addTodo(String todo) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(backendUrl + "/todos"))
                .header("Content-Type", MediaType.TEXT_PLAIN)
                .POST(HttpRequest.BodyPublishers.ofString(todo))
                .build();
        return forward(request, HttpResponse.BodyHandlers.discarding());
    }

    private Response forward(HttpRequest request, HttpResponse.BodyHandler<?> bodyHandler) {
        try {
            HttpResponse<?> response = client.send(request, bodyHandler);
            return Response.status(response.statusCode()).entity(response.body()).build();
        } catch (java.io.IOException e) {
            return Response.status(Response.Status.BAD_GATEWAY).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Response.status(Response.Status.BAD_GATEWAY).build();
        }
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