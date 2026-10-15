package broadcaster;

public record TodoEvent(String action, Long id, String value, boolean done) {

    public String describe() {
        return switch (action) {
            case "created" -> "A todo was created: " + value;
            case "updated" -> (done ? "A todo was marked done: " : "A todo was marked not done: ") + value;
            default -> "A todo was " + action + ": " + value;
        };
    }
}
