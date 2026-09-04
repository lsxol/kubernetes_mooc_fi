package todobackend;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import io.quarkus.logging.Log;

@Path("/todos")
public class TodoBackend {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public List<String> getTodos() {
        List<Todo> todos = Todo.listAll();
        return todos.stream().map(todo -> todo.value).toList();
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
}
