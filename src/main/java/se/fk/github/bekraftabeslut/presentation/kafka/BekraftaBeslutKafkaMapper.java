package se.fk.github.bekraftabeslut.presentation.kafka;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.bekraftabeslut.logic.dto.CreateBekraftaBeslutDataRequest;
import se.fk.github.bekraftabeslut.logic.dto.ImmutableCreateBekraftaBeslutDataRequest;
import se.fk.github.bekraftabeslut.logic.dto.ImmutableUpdateBekraftaBeslutDataRequest;
import se.fk.github.bekraftabeslut.logic.dto.ImmutableUpdateStatusRequest;
import se.fk.github.bekraftabeslut.logic.dto.UpdateBekraftaBeslutDataRequest;
import se.fk.github.bekraftabeslut.logic.dto.UpdateStatusRequest;
import se.fk.github.bekraftabeslut.logic.dto.UppgiftStatus;
import se.fk.rimfrost.OperativtUppgiftslagerResponseMessage;
import se.fk.rimfrost.OperativtUppgiftslagerStatusMessage;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.regel.bekraftabeslut.BekraftaBeslutRequestMessagePayload;

@ApplicationScoped
public class BekraftaBeslutKafkaMapper
{
   public CreateBekraftaBeslutDataRequest toCreateBekraftaBeslutDataRequest(
         BekraftaBeslutRequestMessagePayload BekraftaBeslutRequest)
   {
      return ImmutableCreateBekraftaBeslutDataRequest.builder()
            .id(UUID.fromString(BekraftaBeslutRequest.getId()))
            .kogitorootprociid(UUID.fromString(BekraftaBeslutRequest.getKogitorootprociid()))
            .kogitorootprocid(BekraftaBeslutRequest.getKogitorootprocid())
            .kogitoparentprociid(UUID.fromString(BekraftaBeslutRequest.getKogitoparentprociid()))
            .kogitoprocid(BekraftaBeslutRequest.getKogitoprocid())
            .kogitoprocinstanceid(UUID.fromString(BekraftaBeslutRequest.getKogitoprocinstanceid()))
            .kogitoprocist(BekraftaBeslutRequest.getKogitoprocist())
            .kogitoprocversion(BekraftaBeslutRequest.getKogitoprocversion())
            .kundbehovsflodeId(UUID.fromString(BekraftaBeslutRequest.getData().getKundbehovsflodeId()))
            .build();
   }

   public UpdateBekraftaBeslutDataRequest toUpdateBekraftaBeslutDataRequest(OperativtUppgiftslagerResponseMessage oulResponse)
   {
      return ImmutableUpdateBekraftaBeslutDataRequest.builder()
            .kundbehovsflodeId(UUID.fromString(oulResponse.getKundbehovsflodeId()))
            .uppgiftId(UUID.fromString(oulResponse.getUppgiftId()))
            .build();
   }

   public UpdateStatusRequest toUpdateStatusRequest(OperativtUppgiftslagerStatusMessage statusMessage)
   {

      return ImmutableUpdateStatusRequest.builder()
            .kundbehovsflodeId(UUID.fromString(statusMessage.getKundbehovsflodeId()))
            .uppgiftId(UUID.fromString(statusMessage.getUppgiftId()))
            .uppgiftStatus(mapStatus(statusMessage.getStatus()))
            .build();
   }

   private UppgiftStatus mapStatus(Status status)
   {

      switch (status)
      {
         case NY:
            return UppgiftStatus.NY;
         case TILLDELAD:
            return UppgiftStatus.TILLDELAD;
         case AVSLUTAD:
         default:
            return UppgiftStatus.AVSLUTAD;
      }
   }
}
