package todobackend;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;

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
             .body("value", hasItem("Learn Kubernetes"))
             .body("find { it.value == 'Learn Kubernetes' }.done", is(false));
    }

    @Test
    void todoCanBeMarkedDone() {
        given()
          .contentType(ContentType.TEXT)
          .body("Deploy to the cluster")
          .when().post("/todos")
          .then()
             .statusCode(204);

        Integer id = given()
          .when().get("/todos")
          .then()
             .statusCode(200)
             .extract().path("find { it.value == 'Deploy to the cluster' }.id");

        given()
          .contentType(ContentType.JSON)
          .body("{\"done\": true}")
          .when().put("/todos/" + id)
          .then()
             .statusCode(200)
             .body("id", is(id))
             .body("done", is(true));

        given()
          .when().get("/todos")
          .then()
             .statusCode(200)
             .body("find { it.value == 'Deploy to the cluster' }.done", is(true));
    }

    @Test
    void unknownTodoIsNotFound() {
        given()
          .contentType(ContentType.JSON)
          .body("{\"done\": true}")
          .when().put("/todos/999999")
          .then()
             .statusCode(404);
    }

}
