package se.fk.github.bekraftabeslut;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;

import java.util.HashMap;
import java.util.Map;

public class InMemoryKafkaTestResource implements QuarkusTestResourceLifecycleManager
{

   @Override
   public Map<String, String> start()
   {
      Map<String, String> props = new HashMap<>();

      // Switch the app’s incoming channels to in-memory
      props.putAll(InMemoryConnector.switchIncomingChannelsToInMemory(
            "regel-requests",
            "operativt-uppgiftslager-responses",
            "operativt-uppgiftslager-status-notification"));

      // Switch the app’s outgoing channels to in-memory
      props.putAll(InMemoryConnector.switchOutgoingChannelsToInMemory(
            "operativt-uppgiftslager-requests",
            "operativt-uppgiftslager-status-control"));

      return props;
   }

   @Override
   public void stop()
   {
      InMemoryConnector.clear();
   }
}
