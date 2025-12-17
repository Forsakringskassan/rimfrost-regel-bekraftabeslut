package se.fk.github.manuellregelratttillforsakring.presentation.kafka;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import se.fk.github.logging.callerinfo.model.MDCKeys;
import se.fk.github.manuellregelratttillforsakring.logic.RtfService;
import se.fk.rimfrost.OperativtUppgiftslagerResponseMessage;
import se.fk.rimfrost.OperativtUppgiftslagerStatusMessage;
import se.fk.rimfrost.regel.rtf.manuell.RtfManuellRequestMessagePayload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@ApplicationScoped
public class RtfManuellConsumer
{

   private static final Logger LOGGER = LoggerFactory.getLogger(RtfManuellConsumer.class);

   @Inject
   RtfService rtfService;

   @Inject
   RtfManuellKafkaMapper mapper;

   @Incoming("rtf-manuell-requests")
   public void onRtfManuellRequest(RtfManuellRequestMessagePayload rtfRequest)
   {
      MDC.put(MDCKeys.PROCESSID.name(), rtfRequest.getData().getKundbehovsflodeId());
      LOGGER.info(
            "RtfManuellRequestMessagePayload received with KundbehovsflodeId: " + rtfRequest.getData().getKundbehovsflodeId());

      var request = mapper.toCreateRtfDataRequest(rtfRequest);
      rtfService.createRtfData(request);
   }

   @Incoming("operativt-uppgiftslager-responses")
   public void onOulResponse(OperativtUppgiftslagerResponseMessage oulResponse)
   {
      LOGGER.info("OperativtUppgiftslagerResponseMessage received with KundbehovsflodeId: " + oulResponse.getKundbehovsflodeId());
      var request = mapper.toUpdateRtfDataRequest(oulResponse);
      rtfService.updateRtfData(request);
   }

   @Incoming("operativt-uppgiftslager-status-notification")
   public void onOulStatusMessage(OperativtUppgiftslagerStatusMessage statusMessage)
   {
      LOGGER.info("OperativtUppgiftslagerStatusMessage received with UppgiftId: " + statusMessage.getUppgiftId());
      var request = mapper.toUpdateStatusRequest(statusMessage);
      rtfService.updateStatus(request);
   }

}
