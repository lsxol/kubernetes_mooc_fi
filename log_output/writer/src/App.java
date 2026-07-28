import java.time.Instant;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class App {
    public static void main(String[] args) throws Exception {
        String uuid = UUID.randomUUID().toString();
        Path path = Path.of("/usr/src/app/files/log.txt");
        Files.createDirectories(path.getParent());
        while (true) {
            String line = Instant.now() + ": " + uuid + "\n";
            Files.writeString(path, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            Thread.sleep(5000);
        }
    }
}
