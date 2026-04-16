package se.fk.github.bekraftabeslut;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.http.ContentType;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import jakarta.inject.Inject;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.Idtyp;
import se.fk.rimfrost.OperativtUppgiftslagerRequestMessage;
import se.fk.rimfrost.OperativtUppgiftslagerResponseMessage;
import se.fk.rimfrost.OperativtUppgiftslagerStatusMessage;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.framework.regel.RegelRequestMessagePayload;
import se.fk.rimfrost.framework.regel.RegelRequestMessagePayloadData;
import se.fk.rimfrost.framework.regel.RegelResponseMessagePayload;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.PutHandlaggningRequest;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Referensdata;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Beslut;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.PatchDataRequest;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.UpdateErsattning;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockTestResource.class),
      @QuarkusTestResource(InMemoryKafkaTestResource.class),
      @QuarkusTestResource(StorageDataTestResource.class)
})
class BekraftaBeslutTest
{

   private static final String oulRequestsChannel = "operativt-uppgiftslager-requests";
   private static final String oulResponsesChannel = "operativt-uppgiftslager-responses";
   private static final String oulStatusNotificationChannel = "operativt-uppgiftslager-status-notification";
   private static final String oulStatusControlChannel = "operativt-uppgiftslager-status-control";
   private static final String regelRequestsChannel = "regel-requests";
   private static final String regelResponsesChannel = "regel-responses";
   private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
         .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
   private static final String handlaggningEndpoint = "/handlaggning/";
   private static WireMockServer wiremockServer;

   @BeforeAll
   static void setup()
   {
      setupBekraftaBeslutTest();
   }

   @Test
   public void testHealthEndpoint()
   {
      when()
            .get("/q/health/live")
            .then()
            .statusCode(200)
            .body("status", is("UP"));
   }

   static void setupWiremock()
   {
      wiremockServer = WireMockTestResource.getWireMockServer();
   }

   @Inject
   @Connector("smallrye-in-memory")
   InMemoryConnector inMemoryConnector;

   public static List<LoggedRequest> waitForWireMockRequest(
         WireMockServer server,
         String urlRegex,
         int minRequests)
   {
      List<LoggedRequest> requests = Collections.emptyList();
      int retries = 20;
      long sleepMs = 250;
      for (int i = 0; i < retries; i++)
      {
         requests = server.findAll(anyRequestedFor(urlMatching(urlRegex)));
         if (requests.size() >= minRequests)
         {
            return requests;
         }
         try
         {
            Thread.sleep(sleepMs);
         }
         catch (InterruptedException e)
         {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for WireMock request", e);
         }
      }
      return requests;
   }

   private void sendBekraftaBeslutRequest(String handlaggningId) throws Exception
   {
      RegelRequestMessagePayload payload = new RegelRequestMessagePayload();
      RegelRequestMessagePayloadData data = new RegelRequestMessagePayloadData();
      data.setHandlaggningId(handlaggningId);
      data.setAktivitetId("ff118fd4-a7f2-454c-90b3-7282b7c1fca8");
      payload.setSpecversion(se.fk.rimfrost.framework.regel.SpecVersion.NUMBER_1_DOT_0);
      payload.setId("99994567-89ab-4cde-9012-3456789abcde");
      payload.setSource("TestSource-001");
      payload.setType(regelRequestsChannel);
      payload.setKogitoprocid("234567");
      payload.setKogitorootprocid("123456");
      payload.setKogitorootprociid("77774567-89ab-4cde-9012-3456789abcde");
      payload.setKogitoparentprociid("88884567-89ab-4cde-9012-3456789abcde");
      payload.setKogitoprocinstanceid("66664567-89ab-4cde-9012-3456789abcde");
      payload.setKogitoprocist("345678");
      payload.setKogitoprocversion("111");
      payload.setKogitoproctype(se.fk.rimfrost.framework.regel.KogitoProcType.BPMN);
      payload.setKogitoprocrefid("56789");
      payload.setData(data);
      inMemoryConnector.source(regelRequestsChannel).send(payload);
   }

   static void setupBekraftaBeslutTest()
   {
      Properties props = new Properties();
      try (InputStream in = BekraftaBeslutTest.class.getResourceAsStream("/test.properties"))
      {
         if (in == null)
         {
            throw new RuntimeException("Could not find /test.properties in classpath");
         }
         props.load(in);
      }
      catch (IOException e)
      {
         throw new RuntimeException("Failed to load test.properties", e);
      }
   }

   private List<? extends Message<?>> waitForMessages(String channel)
   {
      await().atMost(5, TimeUnit.SECONDS).until(() -> !inMemoryConnector.sink(channel).received().isEmpty());
      return inMemoryConnector.sink(channel).received();
   }

   private GetDataResponse sendGetBekraftaBeslut(String handlaggningId)
   {
      return given().when().get("/regel/bekraftabeslut/{handlaggningId}", handlaggningId).then().statusCode(200).extract()
            .as(GetDataResponse.class);
   }

   private List<Referensdata> sendGetYrkandestatus()
   {
      return given().when().get("/regel/bekraftabeslut/yrkandestatus").then().statusCode(200).extract().body().jsonPath()
            .getList(".", Referensdata.class);
   }

   private List<Referensdata> sendGetAvslutstyp()
   {
      return given().when().get("/regel/bekraftabeslut/avslutstyp").then().statusCode(200).extract().body().jsonPath()
            .getList(".", Referensdata.class);
   }

   private List<Referensdata> sendGetBeslutstyp()
   {
      return given().when().get("/regel/bekraftabeslut/beslutstyp").then().statusCode(200).extract().body().jsonPath()
            .getList(".", Referensdata.class);
   }

   private List<Referensdata> sendGetBeslutsutfallstyp()
   {
      return given().when().get("/regel/bekraftabeslut/beslutsutfallstyp").then().statusCode(200).extract().body().jsonPath()
            .getList(".", Referensdata.class);
   }

   private void sendPatchBekraftaBeslut(String handlaggningId, PatchDataRequest patchDataRequest)
   {
      given().contentType(ContentType.JSON).body(patchDataRequest).when()
            .patch("/regel/bekraftabeslut/{handlaggningId}/", handlaggningId)
            .then().statusCode(204);
   }

   private void sendPostBekraftaBeslut(String handlaggningId)
   {
      given().when().post("/regel/bekraftabeslut/{handlaggningId}/done", handlaggningId).then().statusCode(204);
   }

   private void verifyReferensdataList(List<Referensdata> referensdataList, List<String> expectedIdsList)
   {
      assertEquals(expectedIdsList.size(), referensdataList.size());

      for (int i = 0; i < expectedIdsList.size(); i++)
      {
         assertEquals(expectedIdsList.get(i), referensdataList.get(i).getId());
      }
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void BekraftaBeslutSmokeTest(String handlaggningId) throws Exception
   {
      setupWiremock();
      // Send regel request to start workflow
      sendBekraftaBeslutRequest(handlaggningId); // TODO metoden kan göras re-usable för alla regler
      //
      // Verify GET handläggning requested
      //
      List<LoggedRequest> handlaggningRequests = waitForWireMockRequest(wiremockServer,
            handlaggningEndpoint + handlaggningId, 1);
      var getRequests = handlaggningRequests.stream().filter(p -> p.getMethod().equals(RequestMethod.GET)).toList();
      assertFalse(getRequests.isEmpty());
      wiremockServer.resetRequests();
      //
      // Verify oul message produced
      //
      var messages = waitForMessages(oulRequestsChannel);
      assertEquals(1, messages.size());
      var oulRequestMessage = (OperativtUppgiftslagerRequestMessage) messages.getFirst().getPayload();
      assertEquals("Bekräfta beslut", oulRequestMessage.getBeskrivning());
      assertEquals(handlaggningId, oulRequestMessage.getHandlaggningId());
      //
      // Send mocked OUL response
      //
      OperativtUppgiftslagerResponseMessage oulResponseMessage = new OperativtUppgiftslagerResponseMessage();
      oulResponseMessage.setHandlaggningId(handlaggningId);
      oulResponseMessage.setUppgiftId("11e53b18-e9ac-4707-825b-a1cb80689c29");
      inMemoryConnector.source(oulResponsesChannel).send(oulResponseMessage);
      //
      // mock status update from OUL
      //
      Idtyp utforare = new Idtyp();
      utforare.setTypId(UUID.randomUUID().toString());
      utforare.setVarde(UUID.randomUUID().toString());

      OperativtUppgiftslagerStatusMessage oulStatusMessage = new OperativtUppgiftslagerStatusMessage();
      oulStatusMessage.setStatus(Status.NY);
      oulStatusMessage.setUppgiftId(oulResponseMessage.getUppgiftId());
      oulStatusMessage.setHandlaggningId(handlaggningId);
      oulStatusMessage.setUtforarId(utforare);
      inMemoryConnector.source(oulStatusNotificationChannel).send(oulStatusMessage);
      //
      // mock GET operation requested from portal FE
      //
      var getBekraftaBeslutResponse = sendGetBekraftaBeslut(handlaggningId);
      //
      // Verify GET operation response
      //
      assertEquals(handlaggningId, getBekraftaBeslutResponse.getHandlaggningId().toString());
      //
      // mock GET operation from rule FE for yrkandestatus list
      //
      var yrkandestatusList = sendGetYrkandestatus();
      //
      // Verify yrkandestatus list content
      //
      verifyReferensdataList(yrkandestatusList, List.of("680e774e-f4f6-4158-9214-32f293a300aa",
            "d07c1851-ad48-4333-abab-57d5507f0088", "16bd3d95-8fad-4f9c-a87a-ba146db55c9b"));
      //
      // mock GET operation from rule FE for avslutstyp list
      //
      var avslutstypList = sendGetAvslutstyp();
      //
      // Verify avslutstyp list content
      //
      verifyReferensdataList(avslutstypList, List.of("dcfb24d5-b0a9-4519-86e5-aece09d85ca9",
            "01ea4c9f-50da-4b99-8fc3-6aee6f4b506d", "b1364919-6002-4e37-a759-556bd2ec4bfd"));
      //
      // mock GET operation from rule FE for beslutstyp list
      //
      var beslutstypList = sendGetBeslutstyp();
      //
      // Verify beslutstyp list content
      //
      verifyReferensdataList(beslutstypList, List.of("c61bf2cd-3f11-4629-8bc2-6ab9ca72666a",
            "e44d5e15-5231-4857-b992-5b8d9c34e36f", "04ece3fc-9015-412e-b48f-eb3ac6d6c846"));
      //
      // mock GET operation from rule FE for beslutsutfallstyp list
      //
      var beslutsutfallList = sendGetBeslutsutfallstyp();
      //
      // Verify beslutsutfallstyp list content
      //
      verifyReferensdataList(beslutsutfallList, List.of("f011934d-d05e-404d-95ab-24571eec241b",
            "1429cc69-1e9d-49be-be41-8e5e4e05a0f9", "5922e22f-4872-4757-938c-5925ef650742"));
      //
      // mock PATCH operation from portal FE
      //
      UpdateErsattning updateErsattning = new UpdateErsattning();
      updateErsattning.setYrkandestatus("ff483a54-0909-4947-b0ec-a0618b861e70"); // FASTSTÄLLT
      updateErsattning.setErsattningId(UUID.fromString("bc6e5150-29be-4758-a2ee-e241f1d7ccf7"));

      Beslut beslut = new Beslut();
      beslut.setAvslutstyp("0b9c0048-34a1-4b69-ace7-1e68fe33c2de");
      beslut.setBeslutstyp("856cbfb3-c1c7-480e-94ee-42b7291848ca");
      beslut.setBeslutsutfall("459bfc9e-c0d4-40d8-b9b5-56ec0801332c");

      PatchDataRequest patchDataRequest = new PatchDataRequest();
      patchDataRequest.setErsattningar(List.of(updateErsattning));
      patchDataRequest.setBeslut(beslut);
      sendPatchBekraftaBeslut(handlaggningId, patchDataRequest);
      wiremockServer.resetRequests();
      //
      // mock POST operation from portal FE
      //
      sendPostBekraftaBeslut(handlaggningId);
      //
      // verify that rule performed requests to handlaggning
      //
      handlaggningRequests = waitForWireMockRequest(wiremockServer, handlaggningEndpoint + handlaggningId, 4);
      var putRequests = handlaggningRequests.stream().filter(p -> p.getMethod().equals(RequestMethod.PUT)).toList();
      assertEquals(2, putRequests.size());
      var regelSentJson = putRequests.getFirst().getBodyAsString();
      var frameworkSentJson = putRequests.getLast().getBodyAsString();
      var regelSentPutHandlaggningRequest = mapper.readValue(regelSentJson, PutHandlaggningRequest.class);
      var frameworkSentPutHandlaggningRequest = mapper.readValue(frameworkSentJson, PutHandlaggningRequest.class);
      assertEquals("16bd3d95-8fad-4f9c-a87a-ba146db55c9b",
            regelSentPutHandlaggningRequest.getHandlaggning().getYrkande().getYrkandestatus());
      assertEquals("3", frameworkSentPutHandlaggningRequest.getHandlaggning().getUppgift().getUppgiftStatus());
      wiremockServer.resetRequests();
      //
      // verify kafka status message sent to oul
      //
      messages = waitForMessages(oulStatusControlChannel);
      assertEquals(1, messages.size());
      oulStatusMessage = (OperativtUppgiftslagerStatusMessage) messages.getFirst().getPayload();
      assertEquals(oulResponseMessage.getUppgiftId(), oulStatusMessage.getUppgiftId());
      assertEquals(Status.AVSLUTAD, oulStatusMessage.getStatus());
      //
      // verify kafka response message sent to VAH
      //
      messages = waitForMessages(regelResponsesChannel);
      assertEquals(1, messages.size());
      var regelResponseMessagePayload = (RegelResponseMessagePayload) messages.getFirst().getPayload();
      assertEquals(handlaggningId, regelResponseMessagePayload.getData().getHandlaggningId());
      assertEquals(Utfall.JA, regelResponseMessagePayload.getData().getUtfall());
   }

}
