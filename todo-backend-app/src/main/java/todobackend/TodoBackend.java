package todobackend;

import io.quarkus.panache.common.Sort;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import io.quarkus.logging.Log;

@Path("/todos")
public class TodoBackend {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public List<Todo> getTodos() {
        return Todo.listAll(Sort.by("id"));
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional
    public void addTodo(String todo) {
        Log.info("Adding new todo: " + todo);
        if (todo.trim().length() > 140) {
            Log.error("To do: " + todo + " is too long");
            return;
        }
        Todo newTodo = new Todo();
        newTodo.value = todo.trim();
        newTodo.persist();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Todo updateTodo(@PathParam("id") Long id, Todo update) {
        Todo todo = Todo.findById(id);
        if (todo == null) {
            Log.error("Todo " + id + " does not exist");
            throw new NotFoundException();
        }
        Log.info("Setting todo " + id + " done: " + update.done);
        todo.done = update.done;
        return todo;
    }
}
