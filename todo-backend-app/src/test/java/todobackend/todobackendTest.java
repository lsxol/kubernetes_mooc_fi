package todobackend;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;

@QuarkusTest
class todobackendTest {

    @Test
    void postedTodoIsListed() {
        given()
          .contentType(ContentType.TEXT)
          .body("Learn Kubernetes")
          .when().post("/todos")
          .then()
             .statusCode(204);

        given()
          .when().get("/todos")
          .then()
             .statusCode(200)
             .contentType(ContentType.JSON)
             .body("$", hasItem("Learn Kubernetes"));
    }

}
