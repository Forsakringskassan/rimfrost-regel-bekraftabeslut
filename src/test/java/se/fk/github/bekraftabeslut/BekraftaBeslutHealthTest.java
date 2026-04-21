package se.fk.github.bekraftabeslut;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.framework.regel.manuell.RegelManuellTestBase;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class BekraftaBeslutHealthTest extends RegelManuellTestBase
{
   @Test
   void health_status_should_be_up()
   {
      when()
            .get("/q/health/live")
            .then()
            .statusCode(200)
            .body("status", is("UP"));
   }
}
