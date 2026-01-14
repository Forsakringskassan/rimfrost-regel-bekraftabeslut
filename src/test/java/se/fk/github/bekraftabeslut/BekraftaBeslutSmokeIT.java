package se.fk.github.bekraftabeslut;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.OperativtUppgiftslagerRequestMessage;
import se.fk.rimfrost.OperativtUppgiftslagerResponseMessage;
import se.fk.rimfrost.OperativtUppgiftslagerStatusMessage;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.PutKundbehovsflodeRequest;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.UppgiftStatus;
import se.fk.rimfrost.regel.bekraftabeslut.*;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Beslutsutfall;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.PatchDataRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("deprecation")
@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
@QuarkusTestResource(WireMockTestResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // så @BeforeAll kan vara icke-static
public class BekraftaBeslutSmokeIT
{

   private static final ObjectMapper mapper = new ObjectMapper()
         .registerModule(new JavaTimeModule())
         .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

   @TestHTTPResource
   URI baseUri;

   private static final HttpClient httpClient = HttpClient.newHttpClient();

   private WireMock wiremockClient;

   private static final String bekraftabeslutRequestsTopic = TestConfig.get("bekraftabeslut.requests.topic");
   private static final String bekraftabeslutResponsesTopic = TestConfig.get("bekraftabeslut.responses.topic");
   private static final String oulRequestsTopic = TestConfig.get("oul.requests.topic");
   private static final String oulResponsesTopic = TestConfig.get("oul.responses.topic");
   private static final String oulStatusNotificationTopic = TestConfig.get("oul.status-notification.topic");
   private static final String oulStatusControlTopic = TestConfig.get("oul.status-control.topic");

   private static final String kundbehovsflodeEndpoint = "/kundbehovsflode/";

   @BeforeAll
   void setup()
   {
      // WireMock port kommer från TestResource
      wiremockClient = new WireMock("localhost", WireMockTestResource());
      WireMock.configureFor("localhost", WireMockTestResource.getPort());

      // Kafka bootstrap från TestResource
      try
      {
         createTopic(bekraftabeslutRequestsTopic, 1, (short) 1);
         createTopic(bekraftabeslutResponsesTopic, 1, (short) 1);
         createTopic(oulRequestsTopic, 1, (short) 1);
         createTopic(oulResponsesTopic, 1, (short) 1);
         createTopic(oulStatusNotificationTopic, 1, (short) 1);
         createTopic(oulStatusControlTopic, 1, (short) 1);
      }
      catch (Exception e)
      {
         throw new RuntimeException("Failed to create Kafka topics", e);
      }
   }

   // ---------------- Kafka helpers ----------------

   static void createTopic(String topicName, int numPartitions, short replicationFactor) throws Exception
   {
      String bootstrap = KafkaTestResource.getBootstrapServers().replace("PLAINTEXT://", "");

      Properties props = new Properties();
      props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);

      try (AdminClient admin = AdminClient.create(props))
      {
         NewTopic topic = new NewTopic(topicName, numPartitions, replicationFactor);
         admin.createTopics(List.of(topic)).all().get();
         System.out.printf("Created topic: %s%n", topicName);
      }
   }

   private KafkaConsumer<String, String> createConsumer()
   {
      Properties props = new Properties();
      props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestResource.getBootstrapServers().replace("PLAINTEXT://", ""));
      props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + System.currentTimeMillis());
      props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
      props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
      props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
      return new KafkaConsumer<>(props);
   }

   private String readKafkaMessage(String topic)
   {
      Properties props = new Properties();
      props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestResource.getBootstrapServers().replace("PLAINTEXT://", ""));
      props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + System.currentTimeMillis());
      props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
      props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
      props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

      try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props))
      {
         consumer.subscribe(Collections.singletonList(topic));
         ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(120));
         if (records.isEmpty())
            throw new IllegalStateException("No Kafka message received on topic " + topic);
         return records.iterator().next().value();
      }
   }

   private void sendBekraftaBeslutRequest(String kundbehovsflodeId) throws Exception
   {
      BekraftaBeslutRequestMessagePayload payload = new BekraftaBeslutRequestMessagePayload();
      BekraftaBeslutRequestMessageData data = new BekraftaBeslutRequestMessageData();
      data.setKundbehovsflodeId(kundbehovsflodeId);

      payload.setSpecversion(SpecVersion.NUMBER_1_DOT_0);
      payload.setId("99994567-89ab-4cde-9012-3456789abcde");
      payload.setSource("TestSource-001");
      payload.setType(bekraftabeslutRequestsTopic);
      payload.setKogitoprocid("234567");
      payload.setKogitorootprocid("123456");
      payload.setKogitorootprociid("77774567-89ab-4cde-9012-3456789abcde");
      payload.setKogitoparentprociid("88884567-89ab-4cde-9012-3456789abcde");
      payload.setKogitoprocinstanceid("66664567-89ab-4cde-9012-3456789abcde");
      payload.setKogitoprocist("345678");
      payload.setKogitoprocversion("111");
      payload.setKogitoproctype(KogitoProcType.BPMN);
      payload.setKogitoprocrefid("56789");
      payload.setData(data);

      String eventJson = mapper.writeValueAsString(payload);

      Properties props = new Properties();
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestResource.getBootstrapServers().replace("PLAINTEXT://", ""));
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

      try (KafkaProducer<String, String> producer = new KafkaProducer<>(props))
      {
         producer.send(new ProducerRecord<>(bekraftabeslutRequestsTopic, eventJson)).get();
      }
   }

   public record OulCorrelation(String kundbehovsflodeId, String uppgiftId, String kafkaKey)
   {
   }

   private ConsumerRecord<String, String> pollForKafkaMessage(KafkaConsumer<String, String> consumer, String topic)
   {
      long deadline = System.currentTimeMillis() + 30000;
      while (System.currentTimeMillis() < deadline)
      {
         ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
         if (!records.isEmpty())
            return records.iterator().next();
      }
      throw new IllegalStateException("No Kafka message received on " + topic);
   }

   private CompletableFuture<OulCorrelation> startKafkaResponderOul(ExecutorService executor)
   {
      return CompletableFuture.supplyAsync(() -> {
         try (KafkaConsumer<String, String> consumer = createConsumer())
         {
            consumer.subscribe(Collections.singletonList(oulRequestsTopic));

            ConsumerRecord<String, String> record = pollForKafkaMessage(consumer, oulRequestsTopic);

            OperativtUppgiftslagerRequestMessage request = mapper.readValue(record.value(),
                  OperativtUppgiftslagerRequestMessage.class);

            String kundbehovsflodeId = request.getKundbehovsflodeId();
            String uppgiftId = UUID.randomUUID().toString();

            OperativtUppgiftslagerResponseMessage responseMessage = new OperativtUppgiftslagerResponseMessage();
            responseMessage.setKundbehovsflodeId(kundbehovsflodeId);
            responseMessage.setUppgiftId(uppgiftId);

            sendOulResponse(record.key(), oulResponsesTopic, responseMessage);

            return new OulCorrelation(kundbehovsflodeId, uppgiftId, record.key());
         }
         catch (Exception e)
         {
            throw new RuntimeException("Kafka responder failed", e);
         }
      }, executor);
   }

   public void sendOulResponse(String key, String topic, OperativtUppgiftslagerResponseMessage response) throws Exception
   {
      String eventJson = mapper.writeValueAsString(response);

      Properties props = new Properties();
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestResource.getBootstrapServers().replace("PLAINTEXT://", ""));
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

      try (KafkaProducer<String, String> producer = new KafkaProducer<>(props))
      {
         producer.send(new ProducerRecord<>(topic, key, eventJson)).get();
      }
   }

   public void sendOulStatus(String key, String topic, OperativtUppgiftslagerStatusMessage response) throws Exception
   {
      String eventJson = mapper.writeValueAsString(response);

      Properties props = new Properties();
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestResource.getBootstrapServers().replace("PLAINTEXT://", ""));
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

      try (KafkaProducer<String, String> producer = new KafkaProducer<>(props))
      {
         producer.send(new ProducerRecord<>(topic, key, eventJson)).get();
      }
   }

   // ---------------- WireMock helper ----------------

   public static List<LoggedRequest> waitForWireMockRequest(WireMock wiremockClient, String urlRegex, int minRequests)
   {
      List<LoggedRequest> requests = Collections.emptyList();
      int retries = 20;
      long sleepMs = 250;

      for (int i = 0; i < retries; i++)
      {
         requests = wiremockClient.findAll(WireMock.anyRequestedFor(WireMock.urlMatching(urlRegex)));
         if (requests.size() >= minRequests)
            return requests;

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

   // ---------------- HTTP calls against QuarkusTest ----------------

   public HttpResponse<String> sendGetBekraftaBeslut(String kundbehovsflodeId)
         throws IOException, InterruptedException
   {

      var url = baseUri.toString() + "/regel/bekrafta-beslut-/" + kundbehovsflodeId;

      HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      assertEquals(200, response.statusCode());
      return response;
   }

   public HttpResponse<String> sendPatchBekraftaBeslut(String kundbehovsflodeId, PatchDataRequest patchDataRequest)
         throws IOException, InterruptedException
   {

      var url = baseUri.toString() + "/regel/bekrafta-beslut-/" + kundbehovsflodeId;
      var json = mapper.writeValueAsString(patchDataRequest);

      HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
            .header("Content-Type", "application/json")
            .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      assertEquals(204, response.statusCode());
      return response;
   }

   // ---------------- The test ----------------

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void TestBekraftaBeslutSmoke(String kundbehovsflodeId) throws Exception
   {
      sendBekraftaBeslutRequest(kundbehovsflodeId);

      ExecutorService executorOul = Executors.newSingleThreadExecutor();
      CompletableFuture<OulCorrelation> responderOul = startKafkaResponderOul(executorOul);
      OulCorrelation oulCorrelation = responderOul.join();
      String uppgiftId = oulCorrelation.uppgiftId();

      // Verify GET kundbehovsflöde requested
      List<LoggedRequest> kundbehovsflodeRequests = waitForWireMockRequest(wiremockClient,
            kundbehovsflodeEndpoint + kundbehovsflodeId, 3);

      var getRequests = kundbehovsflodeRequests.stream()
            .filter(p -> p.getMethod().equals(RequestMethod.GET))
            .toList();
      assertFalse(getRequests.isEmpty());

      // Verify oul message produced
      String kafkaMessage = readKafkaMessage(oulRequestsTopic);
      OperativtUppgiftslagerRequestMessage oulRequestMessage = mapper.readValue(kafkaMessage,
            OperativtUppgiftslagerRequestMessage.class);
      assertEquals(kundbehovsflodeId, oulRequestMessage.getKundbehovsflodeId());

      // mock status update from OUL
      OperativtUppgiftslagerStatusMessage statusMessage = new OperativtUppgiftslagerStatusMessage();
      statusMessage.setStatus(Status.NY);
      statusMessage.setUppgiftId(oulCorrelation.uppgiftId());
      statusMessage.setKundbehovsflodeId(oulCorrelation.kundbehovsflodeId());
      sendOulStatus(oulCorrelation.kafkaKey(), oulStatusNotificationTopic, statusMessage);

      // mock GET from portal FE
      var httpResponse = sendGetBekraftaBeslut(kundbehovsflodeId);
      var getDataResponse = mapper.readValue(httpResponse.body(), GetDataResponse.class);
      assertEquals(kundbehovsflodeId, getDataResponse.getKundbehovsflodeId().toString());

      // mock PATCH from portal FE
      PatchDataRequest patch = new PatchDataRequest();
      patch.setErsattningId(UUID.fromString("67c5ded8-7697-41fd-b943-c58a1be15c93"));
      patch.setAvslagsanledning("");
      patch.setSignera(true);
      patch.setBeslutsutfall(Beslutsutfall.JA);
      sendPatchBekraftaBeslut(kundbehovsflodeId, patch);

      // verify kafka status message sent to oul
      var kafkaOulStatusMessage = readKafkaMessage(oulStatusControlTopic);
      OperativtUppgiftslagerStatusMessage oulStatusMessage = mapper.readValue(kafkaOulStatusMessage,
            OperativtUppgiftslagerStatusMessage.class);
      assertEquals(uppgiftId, oulStatusMessage.getUppgiftId());
      assertEquals(Status.AVSLUTAD, oulStatusMessage.getStatus());

      // verify kafka response message sent to VAH
      var kafkaResp = readKafkaMessage(bekraftabeslutResponsesTopic);
      BekraftaBeslutResponseMessagePayload resp = mapper.readValue(kafkaResp, BekraftaBeslutResponseMessagePayload.class);
      assertEquals(kundbehovsflodeId, resp.getData().getKundbehovsflodeId());
      assertEquals(RattTillForsakring.JA, resp.getData().getRattTillForsakring());
   }
}
