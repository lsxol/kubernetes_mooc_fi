package todobackend;

public record TodoEvent(String action, Long id, String value, boolean done) {

    static TodoEvent of(String action, Todo todo) {
        return new TodoEvent(action, todo.id, todo.value, todo.done);
    }
}
