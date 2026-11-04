package se.fk.github.bekraftabeslut;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.ersattningdata.ErsattningData;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.fk.github.bekraftabeslut.BekraftaBeslutRestMock.sendPatchBekraftaBeslut;
import static se.fk.github.bekraftabeslut.BekraftaBeslutTestData.PRODUCERADE_RESULTAT_ID;
import static se.fk.github.bekraftabeslut.BekraftaBeslutTestData.newPatchDataRequest;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockBekraftaBeslut.class)
})
public class BekraftaBeslutPatchDataTest extends AbstractRegelManuellTest
{
   @ConfigProperty(name = "mp.messaging.outgoing.regel-responses.topic")
   String responseTopic;

   @Inject
   ObjectMapper objectMapper;

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void patch_data_should_update_handlaggning(String handlaggningId) throws JsonProcessingException
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);
      waitForRegelManuellReady(handlaggningId);
      //
      // clear wiremock requests
      //
      WireMockBekraftaBeslut.getWireMockServer().resetRequests();
      //
      // mock PATCH operation from portal FE
      //
      sendPatchBekraftaBeslut(handlaggningId, newPatchDataRequest());
      //
      // verify PUT handlaggning
      //
      var handlaggningPutUpdate = WireMockBekraftaBeslut.getLastPutHandlaggningUpdate(handlaggningId);
      assertEquals(handlaggningId, handlaggningPutUpdate.getHandlaggning().getId().toString());
      assertEquals(1, handlaggningPutUpdate.getHandlaggning().getVersion());
      assertEquals("PLANERAD", handlaggningPutUpdate.getUppgift().getUppgiftStatus());
      assertEquals(2, handlaggningPutUpdate.getHandlaggning().getYrkande().getVersion());
      assertEquals(1, handlaggningPutUpdate.getHandlaggning().getYrkande().getSakfragorStallningstaganden().size());
      var sakfragorStallningstagandenContainer = handlaggningPutUpdate.getHandlaggning().getYrkande()
            .getSakfragorStallningstaganden().getFirst();
      var ersattningData = ErsattningData.fromJson(sakfragorStallningstagandenContainer.getData(), objectMapper);
      assertEquals(2, ersattningData.getVersion());
      assertEquals(PRODUCERADE_RESULTAT_ID, sakfragorStallningstagandenContainer.getId().toString());
      assertEquals("16bd3d95-8fad-4f9c-a87a-ba146db55c9b", ersattningData.getYrkandeStatus());
      assertNotNull(handlaggningPutUpdate.getHandlaggning().getYrkande().getBeslut());
      assertEquals(1, handlaggningPutUpdate.getHandlaggning().getYrkande().getBeslut().size());
      var beslut = handlaggningPutUpdate.getHandlaggning().getYrkande().getBeslut().getFirst();
      assertEquals(1, beslut.getVersion());
      assertEquals(1, beslut.getBeslutsrader().size());
      var beslutsrad = beslut.getBeslutsrader().getFirst();
      assertEquals(1, beslutsrad.getVersion());
      assertEquals("b1364919-6002-4e37-a759-556bd2ec4bfd", beslutsrad.getAvslutsTyp());
      assertEquals("e44d5e15-5231-4857-b992-5b8d9c34e36f", beslutsrad.getBeslutsTyp());
      assertEquals("f011934d-d05e-404d-95ab-24571eec241b", beslutsrad.getBeslutsUtfall());
   }

}
