package broadcaster;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TodoEventTest {

    @Test
    void createdTodo() {
        assertEquals("A todo was created: Learn Kubernetes",
                new TodoEvent("created", 1L, "Learn Kubernetes", false).describe());
    }

    @Test
    void todoMarkedDone() {
        assertEquals("A todo was marked done: Learn Kubernetes",
                new TodoEvent("updated", 1L, "Learn Kubernetes", true).describe());
    }

    @Test
    void todoMarkedNotDone() {
        assertEquals("A todo was marked not done: Learn Kubernetes",
                new TodoEvent("updated", 1L, "Learn Kubernetes", false).describe());
    }
}
