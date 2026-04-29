package se.fk.github.bekraftabeslut;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.framework.oul.logic.dto.ImmutableIdtyp;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.fk.github.bekraftabeslut.BekraftaBeslutRestMock.sendGetAvslutstypBekraftaBeslut;
import static se.fk.github.bekraftabeslut.BekraftaBeslutRestMock.sendGetBekraftaBeslut;
import static se.fk.github.bekraftabeslut.BekraftaBeslutRestMock.sendGetBeslutstypBekraftaBeslut;
import static se.fk.github.bekraftabeslut.BekraftaBeslutRestMock.sendGetBeslutsutfallstypBekraftaBeslut;
import static se.fk.github.bekraftabeslut.BekraftaBeslutRestMock.sendPatchBekraftaBeslut;
import static se.fk.github.bekraftabeslut.BekraftaBeslutRestMock.sendPostBekraftaBeslut;
import static se.fk.github.bekraftabeslut.BekraftaBeslutTestData.newPatchDataRequest;
import static se.fk.rimfrost.framework.regel.WireMockHandlaggning.waitForHandlaggningRequests;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockBekraftaBeslut.class)
})
public class BekraftaBeslutSequenceTest extends AbstractRegelManuellTest
{

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29, Idtyp_typId, Idtyp_varde"
   })
   void full_sequence_should_create_regel_response(
         String handlaggningId,
         String uppgiftId,
         String idtypTypId,
         String idtypVarde)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      //
      // Verify GET handläggning requested
      //
      var handlaggningGetRequests = waitForHandlaggningRequests(handlaggningId, RequestMethod.GET, 1);
      assertEquals(1, handlaggningGetRequests.size());
      //
      // Verify oul message produced
      //
      var messages = oulKafkaConnector.waitForMessages(getOulRequestsChannel());
      assertEquals(1, messages.size());
      //
      // Send mocked OUL response
      //
      oulKafkaConnector.simulateOulResponse(handlaggningId, uppgiftId);
      //
      // mock status update from OUL
      //
      var utforarId = ImmutableIdtyp.builder()
            .typId(idtypTypId)
            .varde(idtypVarde)
            .build();
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, utforarId, Status.NY);
      //
      // Verify PUT handlaggning
      //
      var handlaggningPutRequests = WireMockBekraftaBeslut.waitForHandlaggningRequests(handlaggningId, RequestMethod.PUT, 2);
      assertEquals(2, handlaggningPutRequests.size());
      //
      // mock GET operation requested from portal FE
      //
      sendGetBekraftaBeslut(handlaggningId);
      //
      // Verify PUT handlaggning
      //
      handlaggningPutRequests = WireMockBekraftaBeslut.waitForHandlaggningRequests(handlaggningId, RequestMethod.PUT, 3);
      assertEquals(3, handlaggningPutRequests.size());
      //
      // mock GET operation requested from portal FE
      //
      sendGetAvslutstypBekraftaBeslut();
      //
      // mock GET operation requested from portal FE
      //
      sendGetBeslutstypBekraftaBeslut();
      //
      // mock GET operation requested from portal FE
      //
      sendGetBeslutsutfallstypBekraftaBeslut();
      //
      // mock PATCH operation from portal FE
      //
      sendPatchBekraftaBeslut(handlaggningId, newPatchDataRequest());
      //
      // Verify PUT handlaggning
      //
      handlaggningPutRequests = WireMockBekraftaBeslut.waitForHandlaggningRequests(handlaggningId, RequestMethod.PUT, 4);
      assertEquals(4, handlaggningPutRequests.size());
      //
      // mock POST operation from portal FE
      //
      sendPostBekraftaBeslut(handlaggningId);
      //
      // Verify PUT handlaggning
      //
      handlaggningPutRequests = WireMockBekraftaBeslut.waitForHandlaggningRequests(handlaggningId, RequestMethod.PUT, 5);
      assertEquals(6, handlaggningPutRequests.size());
      //
      // verify kafka status message sent to oul
      //
      oulKafkaConnector.waitForOulStatusMessage();
      //
      // Verify produced regel response
      //
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      var regelResponseData = regelResponse.getData();
      assertEquals(handlaggningId, regelResponseData.getHandlaggningId());
      assertEquals(Utfall.JA, regelResponseData.getUtfall());
   }

}
