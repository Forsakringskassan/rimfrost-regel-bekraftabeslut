package se.fk.github.bekraftabeslut;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.PutHandlaggningRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.fk.github.bekraftabeslut.BekraftaBeslutRestMock.sendPostBekraftaBeslut;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockBekraftaBeslut.class)
})
public class BekraftaBeslutPostDataTest extends AbstractRegelManuellTest
{

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void post_data_done_should_update_handlaggning_uppgift_avslutad(String handlaggningId, String uppgiftId)
         throws JsonProcessingException
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      waitForRegelManuellReady(handlaggningId);
      //
      // clear wiremock requests
      //
      WireMockBekraftaBeslut.getWireMockServer().resetRequests();
      //
      // mock POST done operation from portal FE
      //
      sendPostBekraftaBeslut(handlaggningId);
      //
      // verify PUT handlaggning
      //
      var handlaggningPutUpdate = WireMockBekraftaBeslut.getLastPutHandlaggning(handlaggningId);
      assertEquals(handlaggningId, handlaggningPutUpdate.getHandlaggning().getId().toString());
      assertEquals(1, handlaggningPutUpdate.getHandlaggning().getVersion());
      assertEquals(2, handlaggningPutUpdate.getHandlaggning().getUppgift().getVersion());
      assertEquals("AVSLUTAD", handlaggningPutUpdate.getHandlaggning().getUppgift().getUppgiftStatus());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void post_data_done_should_update_oul_status(String handlaggningId, String uppgiftId)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      waitForRegelManuellReady(handlaggningId);
      //
      // mock POST done operation from portal FE
      //
      sendPostBekraftaBeslut(handlaggningId);
      //
      // verify REST call to end uppgift was made
      //
      var endRequests = WireMockBekraftaBeslut.waitForRequest("/uppgifter/" + uppgiftId + "/end", RequestMethod.POST, 1);
      assertEquals(1, endRequests.size());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29, 16bd3d95-8fad-4f9c-a87a-ba146db55c9b"
   })
   void post_data_done_should_update_yrkande_faststalld(String handlaggningId, String uppgiftId, String yrkandestatus)
         throws JsonProcessingException
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      waitForRegelManuellReady(handlaggningId);
      //
      // clear wiremock requests
      //
      WireMockBekraftaBeslut.getWireMockServer().resetRequests();
      //
      // mock POST done operation from portal FE
      //
      sendPostBekraftaBeslut(handlaggningId);
      //
      // verify PUT handlaggning
      //
      var handlaggningPutUpdates = WireMockBekraftaBeslut.waitForHandlaggningRequests(handlaggningId, RequestMethod.PUT, 2);
      assertEquals(2, handlaggningPutUpdates.size());
      var handlaggningPutUpdate = mapper.readValue(handlaggningPutUpdates.getFirst().getBodyAsString(),
            PutHandlaggningRequest.class);
      assertEquals(handlaggningId, handlaggningPutUpdate.getHandlaggning().getId().toString());
      assertEquals(1, handlaggningPutUpdate.getHandlaggning().getVersion());
      assertEquals(yrkandestatus, handlaggningPutUpdate.getHandlaggning().getYrkande().getYrkandestatus());
   }

}
