package se.fk.github.bekraftabeslut;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.framework.regel.manuell.RegelManuellTestBase;
import se.fk.rimfrost.framework.regel.manuell.WireMockRegelManuell;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockBekraftaBeslut.class)
})
public class BekraftaBeslutHandlaggningTest extends RegelManuellTestBase
{

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void should_create_initial_get_handlaggning(String handlaggningId)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      //
      // verify produced initial GET handlaggning
      //
      var handlaggningRequests = WireMockRegelManuell.waitForHandlaggningRequests(handlaggningId, RequestMethod.GET, 1);
      Assertions.assertEquals(1, handlaggningRequests.size());
      Assertions.assertTrue(handlaggningRequests.getFirst().getUrl().contains("handlaggning"));
      Assertions.assertTrue(handlaggningRequests.getFirst().getUrl().contains(handlaggningId));
   }
}
