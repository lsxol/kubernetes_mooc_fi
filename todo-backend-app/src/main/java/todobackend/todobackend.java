package todobackend;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("/todos")
public class todobackend {

    private final List<String> todos = new ArrayList<>();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getTodos() {
        return todos;
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void addTodo(String todo) {
        todos.add(todo);
    }
}
