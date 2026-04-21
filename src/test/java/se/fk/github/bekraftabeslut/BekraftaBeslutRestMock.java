package se.fk.github.bekraftabeslut;

import io.restassured.http.ContentType;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.PatchDataRequest;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Referensdata;

import java.util.List;

import static io.restassured.RestAssured.given;

public class BekraftaBeslutRestMock
{
   public static GetDataResponse sendGetBekraftaBeslut(String handlaggningId)
   {
      return given().when().get("/regel/bekraftabeslut/{handlaggningId}", handlaggningId).then().statusCode(200).extract()
            .as(GetDataResponse.class);
   }

   public static void sendPatchBekraftaBeslut(String handlaggningId, PatchDataRequest patchDataRequest)
   {
      given().contentType(ContentType.JSON).body(patchDataRequest).when()
            .patch("/regel/bekraftabeslut/{handlaggningId}", handlaggningId)
            .then().statusCode(204);
   }

   public static void sendPostBekraftaBeslut(String handlaggningId)
   {
      given().when().post("/regel/bekraftabeslut/{handlaggningId}/done", handlaggningId).then().statusCode(204);
   }

   public static List<Referensdata> sendGetAvslutstypBekraftaBeslut()
   {
      return given().when().get("/regel/bekraftabeslut/avslutstyp").then().statusCode(200).extract()
            .jsonPath().getList(".", Referensdata.class);
   }

   public static List<Referensdata> sendGetBeslutstypBekraftaBeslut()
   {
      return given().when().get("/regel/bekraftabeslut/beslutstyp").then().statusCode(200).extract()
            .jsonPath().getList(".", Referensdata.class);
   }

   public static List<Referensdata> sendGetBeslutsutfallstypBekraftaBeslut()
   {
      return given().when().get("/regel/bekraftabeslut/beslutsutfallstyp").then().statusCode(200).extract()
            .jsonPath().getList(".", Referensdata.class);
   }
}
