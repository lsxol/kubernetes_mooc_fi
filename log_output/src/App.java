import java.time.Instant;
import java.util.UUID;

public class App {
    public static void main(String[] args) throws Exception {
        String uuid = UUID.randomUUID().toString();
        while(true){
            System.out.println(Instant.now() + " " + uuid);
            Thread.sleep(5000);
        }
    }
}
