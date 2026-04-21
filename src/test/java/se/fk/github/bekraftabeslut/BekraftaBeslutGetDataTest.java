package se.fk.github.bekraftabeslut;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.framework.regel.manuell.RegelManuellTestBase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.fk.github.bekraftabeslut.BekraftaBeslutRestMock.sendGetBekraftaBeslut;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockBekraftaBeslut.class)
})
public class BekraftaBeslutGetDataTest extends RegelManuellTestBase
{
   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void get_data_should_contain_handlaggning_id(String handlaggningId)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var getDataResponse = sendGetBekraftaBeslut(handlaggningId);
      Assertions.assertEquals(handlaggningId, getDataResponse.getHandlaggningId().toString());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void get_data_should_contain_ersattningar(String handlaggningId)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var getDataResponse = sendGetBekraftaBeslut(handlaggningId);
      Assertions.assertEquals(1, getDataResponse.getErsattning().size());
      Assertions.assertEquals(BekraftaBeslutTestData.ERSATTNINGSID,
            getDataResponse.getErsattning().getFirst().getErsattningId().toString());
      Assertions.assertEquals(BekraftaBeslutTestData.ERSATTNINGSTYP,
            getDataResponse.getErsattning().getFirst().getErsattningstyp());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void get_data_should_contain_kund(String handlaggningId)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var getDataResponse = sendGetBekraftaBeslut(handlaggningId);
      Assertions.assertEquals(BekraftaBeslutTestData.KUND_FORNAMN, getDataResponse.getKund().getFornamn());
      Assertions.assertEquals(BekraftaBeslutTestData.KUND_EFTERNAMN, getDataResponse.getKund().getEfternamn());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void get_data_should_update_handlaggning(String handlaggningId) throws JsonProcessingException
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      //
      // clear wiremock requests
      //
      WireMockBekraftaBeslut.getWireMockServer().resetRequests();
      //
      // Send bekraftabeslut GET
      //
      sendGetBekraftaBeslut(handlaggningId);
      //
      // verify PUT handlaggning
      //
      var handlaggningPutUpdate = WireMockBekraftaBeslut.getLastPutHandlaggning(handlaggningId);
      assertEquals(handlaggningId, handlaggningPutUpdate.getHandlaggning().getId().toString());
      assertEquals(2, handlaggningPutUpdate.getHandlaggning().getVersion());
      assertEquals("1", handlaggningPutUpdate.getHandlaggning().getUppgift().getUppgiftStatus());
      assertEquals(1, handlaggningPutUpdate.getHandlaggning().getUppgift().getVersion());
   }
}
