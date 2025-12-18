package se.fk.github.bekraftabeslut.presentation.kafka;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import se.fk.github.bekraftabeslut.logic.BekraftaBeslutService;
import se.fk.github.logging.callerinfo.model.MDCKeys;
//import se.fk.rimfrost.OperativtUppgiftslagerResponseMessage;
//import se.fk.rimfrost.OperativtUppgiftslagerStatusMessage;
import se.fk.rimfrost.regel.bekraftabeslut.BekraftaBeslutRequestMessagePayload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@ApplicationScoped
public class BekraftaBeslutConsumer
{

   private static final Logger LOGGER = LoggerFactory.getLogger(BekraftaBeslutConsumer.class);

   @Inject
   BekraftaBeslutService bekraftabeslutService;

   @Inject
   BekraftaBeslutKafkaMapper mapper;

   @Incoming("bekrafta-beslut-requests")
   public void onBekraftaBeslutRequest(BekraftaBeslutRequestMessagePayload bekraftaBeslutRequest)
   {
      MDC.put(MDCKeys.PROCESSID.name(), bekraftaBeslutRequest.getData().getKundbehovsflodeId());
      LOGGER.info(
            "BekraftaBeslutRequestMessagePayload received with KundbehovsflodeId: " + bekraftaBeslutRequest.getData().getKundbehovsflodeId());

      var request = mapper.toCreateBekraftaBeslutDataRequest(bekraftaBeslutRequest);
      bekraftabeslutService.createBekraftaBeslutData(request);
   }

   // @Incoming("operativt-uppgiftslager-responses")
   // public void onOulResponse(OperativtUppgiftslagerResponseMessage oulResponse)
   // {
   //    LOGGER.info("OperativtUppgiftslagerResponseMessage received with KundbehovsflodeId: " + oulResponse.getKundbehovsflodeId());
   //    var request = mapper.toUpdateRtfDataRequest(oulResponse);
   //    rtfService.updateRtfData(request);
   // }

   // @Incoming("operativt-uppgiftslager-status-notification")
   // public void onOulStatusMessage(OperativtUppgiftslagerStatusMessage statusMessage)
   // {
   //    LOGGER.info("OperativtUppgiftslagerStatusMessage received with UppgiftId: " + statusMessage.getUppgiftId());
   //    var request = mapper.toUpdateStatusRequest(statusMessage);
   //    rtfService.updateStatus(request);
   // }

}
