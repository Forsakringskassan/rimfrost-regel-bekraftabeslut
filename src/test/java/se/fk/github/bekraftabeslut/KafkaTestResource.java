package se.fk.github.bekraftabeslut;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

public class KafkaTestResource implements QuarkusTestResourceLifecycleManager
{

   private static KafkaContainer kafka;

   @Override
   public Map<String, String> start()
   {
      kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
      kafka.start();

      return Map.of(
            "kafka.bootstrap.servers", kafka.getBootstrapServers());
   }

   @Override
   public void stop()
   {
      if (kafka != null)
         kafka.stop();
   }

   public static String getBootstrapServers()
   {
      return kafka.getBootstrapServers();
   }
}
