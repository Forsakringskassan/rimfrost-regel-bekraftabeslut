package se.fk.github.bekraftabeslut;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(WireMockTestResource.class) // behåll om du behöver WireMock för uppstart
class BekraftaBeslutSmokeIT {

  @TestHTTPResource
  URI baseUri;

  private static final HttpClient http = HttpClient.newHttpClient();

  @Test
  void app_starts_and_endpoint_responds() throws Exception {
    // Byt path till en endpoint du vet finns (alt /q/health om health är på)
    URI url = baseUri.resolve("/q/health");

    HttpRequest req = HttpRequest.newBuilder(url).GET().build();
    HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, res.statusCode());
  }
}
