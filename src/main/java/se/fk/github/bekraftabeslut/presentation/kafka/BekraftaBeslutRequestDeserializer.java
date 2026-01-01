package se.fk.github.bekraftabeslut.presentation.kafka;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import se.fk.rimfrost.regel.bekraftabeslut.BekraftaBeslutRequestMessagePayload;

public class BekraftaBeslutRequestDeserializer extends ObjectMapperDeserializer<BekraftaBeslutRequestMessagePayload>
{
   public BekraftaBeslutRequestDeserializer()
   {
      super(BekraftaBeslutRequestMessagePayload.class);
   }
}
