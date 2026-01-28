package se.fk.github.bekraftabeslut.integration.kafka;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import se.fk.github.bekraftabeslut.integration.kafka.dto.BekraftaBeslutResponseRequest;
import se.fk.rimfrost.OperativtUppgiftslagerRequestMessage;
import se.fk.rimfrost.OperativtUppgiftslagerStatusMessage;
import se.fk.rimfrost.framework.integration.kafka.dto.ImmutableOulMessageRequest;
import se.fk.rimfrost.regel.bekraftabeslut.BekraftaBeslutResponseMessagePayload;
import se.fk.rimfrost.Status;

@ApplicationScoped
public class BekraftaBeslutKafkaProducer
{
   @Inject
   BekraftaBeslutKafkaMapper mapper;

   @Inject
   @Channel("operativt-uppgiftslager-status-control")
   @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 1024)
   Emitter<OperativtUppgiftslagerStatusMessage> oulStatusEmitter;

   @Inject
   @Channel("bekraftabeslut-responses")
   @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 1024)
   Emitter<BekraftaBeslutResponseMessagePayload> bekraftaBeslutResponseEmitter;

   public void sendOulStatusUpdate(UUID uppgiftId, Status status)
   {
      var message = new OperativtUppgiftslagerStatusMessage();
      message.setUppgiftId(uppgiftId.toString());
      message.setStatus(status);
      oulStatusEmitter.send(message);
   }

   public void sendBekraftaBeslutResponse(BekraftaBeslutResponseRequest bekraftaBeslutResponseRequest)
   {
      var response = mapper.toBekraftaBeslutResponse(bekraftaBeslutResponseRequest);
      bekraftaBeslutResponseEmitter.send(response);
   }

   public void sendOulRequest(ImmutableOulMessageRequest oulMessageRequest) {
      throw new UnsupportedOperationException("Unimplemented method 'sendOulRequest'");
   }
}
