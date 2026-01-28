package se.fk.github.bekraftabeslut;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;

import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static io.restassured.RestAssured.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import se.fk.rimfrost.OperativtUppgiftslagerRequestMessage;
import se.fk.rimfrost.OperativtUppgiftslagerResponseMessage;
import se.fk.rimfrost.OperativtUppgiftslagerStatusMessage;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.PutKundbehovsflodeRequest;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.UppgiftStatus;
import se.fk.rimfrost.regel.common.RegelRequestMessagePayload;
import se.fk.rimfrost.regel.common.RegelRequestMessagePayloadData;
import se.fk.rimfrost.regel.bekraftabeslut.*;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.RegelBekraftaBeslutControllerApi;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.PatchDataRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@QuarkusTest
@QuarkusTestResource(WireMockTestResource.class)
class BekraftaBeslutSmokeIT {

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrapServers;

    @ConfigProperty(name = "mp.messaging.outgoing.operativt-uppgiftslager-requests.topic")
    String oulRequestsTopic;

    @TestHTTPResource
    URI baseUri;

    private static WireMockServer wiremockServer;
    private static WireMock wiremockClient;

    private static final HttpClient http = HttpClient.newHttpClient();

    @BeforeAll
    static void setup() {
        setupBekraftaBeslut();
    }


    @Test
    public void testHealthEndpoint() {
        when()
                .get("/q/health/live")
                .then()
                .statusCode(200)
                .body("status", is("UP")); // JSON returned: {"status":"UP"}
    }

    @Test
    void send_one_kafka_message() throws Exception {
        WireMockServer server =
                WireMockTestResource.getWireMockServer();
        WireMock client = new WireMock(
                "localhost",
                server.port()
        );
        String kundbehovsflodeId = "5367f6b8-cc4a-11f0-8de9-199901011234";
        sendBekraftaBeslutRequest(kundbehovsflodeId);
        List<LoggedRequest> kundbehovsflodeRequests = waitForWireMockRequest(server,
                "(/.*)?", 1);
    }

    static void setupWiremock() {
        wiremockServer = WireMockTestResource.getWireMockServer();
        wiremockClient = new WireMock("localhost", wiremockServer.port());
    }

    @ParameterizedTest
    @CsvSource(
            {
                    "5367f6b8-cc4a-11f0-8de9-199901011234"
            })
    void TestBekraftaBeslutSmokeIT(String kundbehovsflodeId) throws Exception {
        setupWiremock();
        KafkaConsumer<String, String> consumer = createConsumer();
        consumer.subscribe(List.of(oulRequestsTopic));
        sendBekraftaBeslutRequest(kundbehovsflodeId);
        ConsumerRecords<String, String> oulRequests =  waitForOulRequest(consumer, oulRequestsTopic);
        var x = 1;
    }

    private ConsumerRecords<String, String> waitForOulRequest(KafkaConsumer kafkaConsumer, String oulRequestsTopic) {
        AtomicReference<ConsumerRecords<String, String>> recordsRef = new AtomicReference<>();
        await()
                .atMost(10, SECONDS)
                .untilAsserted(() -> {
                    ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(500));
                    assertThat(records.count()).isGreaterThan(0);
                    recordsRef.set(records);
                });
        return recordsRef.get();
    }

    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static GenericContainer<?> bekraftaBeslut;
    private static final String kafkaImage = TestConfig.get("kafka.image");
    private static final String bekraftaBeslutImage = TestConfig.get("bekraftabeslut.image");
    private static final String bekraftaBeslutRequestsTopic = TestConfig.get("bekraftabeslut.requests.topic");
    private static final String bekraftaBeslutResponsesTopic = TestConfig.get("bekraftabeslut.responses.topic");
    private static final String oulResponsesTopic = TestConfig.get("oul.responses.topic");
    private static final String oulStatusNotificationTopic = TestConfig.get("oul.status-notification.topic");
    private static final String oulStatusControlTopic = TestConfig.get("oul.status-control.topic");
    private static final String networkAlias = TestConfig.get("network.alias");
    private static final String smallryeKafkaBootstrapServers = networkAlias + ":9092";
    private static final Network network = Network.newNetwork();
    private static final String wiremockUrl = "http://wiremock:8080";
    private static final String kundbehovsflodeEndpoint = "/kundbehovsflode/";
    private static final HttpClient httpClient = HttpClient.newHttpClient();


    // static void createTopic(String topicName, int numPartitions, short replicationFactor) throws Exception {
    //   Properties props = new Properties();
    //   props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);

    //   try (AdminClient admin = AdminClient.create(props)) {
    //     NewTopic topic = new NewTopic(topicName, numPartitions, replicationFactor);
    //     admin.createTopics(List.of(topic)).all().get();
    //     System.out.printf("Created topic: %s%n", topicName);
    //   } catch (Exception e) {

    //     if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("TopicExists")) {
    //       System.out.printf("Topic already exists: %s%n", topicName);
    //     } else {
    //       throw e;
    //     }
    //   }
    // }
    public static List<LoggedRequest> waitForWireMockRequest(
            WireMockServer server,
            String urlRegex,
            int minRequests) {
        List<LoggedRequest> requests = Collections.emptyList();
        int retries = 20;
        long sleepMs = 250;
        for (int i = 0; i < retries; i++) {
            requests = server.findAll(WireMock.anyRequestedFor(WireMock.urlMatching(urlRegex)));
            if (requests.size() >= minRequests) {
                return requests;
            }
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for WireMock request", e);
            }
        }
        return requests; // empty if nothing received
    }

    private void sendBekraftaBeslutRequest(String kundbehovsflodeId) throws Exception {
        RegelRequestMessagePayload payload = new RegelRequestMessagePayload();
        RegelRequestMessagePayloadData data = new RegelRequestMessagePayloadData();
        data.setKundbehovsflodeId(kundbehovsflodeId);
        payload.setSpecversion(se.fk.rimfrost.regel.common.SpecVersion.NUMBER_1_DOT_0);
        payload.setId("99994567-89ab-4cde-9012-3456789abcde");
        payload.setSource("TestSource-001");
        payload.setType(bekraftaBeslutRequestsTopic);
        payload.setKogitoprocid("234567");
        payload.setKogitorootprocid("123456");
        payload.setKogitorootprociid("77774567-89ab-4cde-9012-3456789abcde");
        payload.setKogitoparentprociid("88884567-89ab-4cde-9012-3456789abcde");
        payload.setKogitoprocinstanceid("66664567-89ab-4cde-9012-3456789abcde");
        payload.setKogitoprocist("345678");
        payload.setKogitoprocversion("111");
        payload.setKogitoproctype(se.fk.rimfrost.regel.common.KogitoProcType.BPMN);
        payload.setKogitoprocrefid("56789");
        payload.setData(data);
        // Serialize entire payload to JSON
        String eventJson = mapper.writeValueAsString(payload);

        // Använda properties??
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    bekraftaBeslutRequestsTopic,
                    eventJson);
            System.out.printf("Kafka sending to topic : %s, json: %s%n", bekraftaBeslutRequestsTopic, eventJson);
            producer.send(record).get();
        }
    }

    private KafkaConsumer<String, String> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + System.currentTimeMillis());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return new KafkaConsumer<>(props);
    }

    static void setupBekraftaBeslut() {
        Properties props = new Properties();
        try (InputStream in = BekraftaBeslutSmokeIT.class.getResourceAsStream("/test.properties")) {
            if (in == null) {
                throw new RuntimeException("Could not find /test.properties in classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test.properties", e);
        }

    }
}
