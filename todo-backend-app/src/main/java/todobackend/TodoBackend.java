package todobackend;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

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
        Todo newTodo = new Todo();
        newTodo.value = todo;
        newTodo.persist();
    }
}
