package se.fk.github.manuellregelratttillforsakring.presentation.kafka;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import se.fk.rimfrost.OperativtUppgiftslagerStatusMessage;

public class OulStatusNotificationDeserializer extends ObjectMapperDeserializer<OperativtUppgiftslagerStatusMessage>
{

   public OulStatusNotificationDeserializer()
   {
      super(OperativtUppgiftslagerStatusMessage.class);
   }

}
