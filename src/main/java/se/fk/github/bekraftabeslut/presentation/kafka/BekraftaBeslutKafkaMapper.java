package se.fk.github.bekraftabeslut.presentation.kafka;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.bekraftabeslut.logic.dto.CreateBekraftaBeslutDataRequest;
import se.fk.rimfrost.regel.bekraftabeslut.BekraftaBeslutRequestMessagePayload;

@ApplicationScoped
public class BekraftaBeslutKafkaMapper {
   public CreateBekraftaBeslutDataRequest toCreateRtfDataRequest(BekraftaBeslutRequestMessagePayload bekraftaBeslutRequest)
   {
      
      return ImmutableCreateBekraftaBeslutDataRequest.builder()
            .id(UUID.fromString(bekraftaBeslutRequest.getId()))
            .kogitorootprociid(UUID.fromString(bekraftaBeslutRequest.getKogitorootprociid()))
            .kogitorootprocid(bekraftaBeslutRequest.getKogitorootprocid())
            .kogitoparentprociid(UUID.fromString(bekraftaBeslutRequest.getKogitoparentprociid()))
            .kogitoprocid(bekraftaBeslutRequest.getKogitoprocid())
            .kogitoprocinstanceid(UUID.fromString(bekraftaBeslutRequest.getKogitoprocinstanceid()))
            .kogitoprocist(bekraftaBeslutRequest.getKogitoprocist())
            .kogitoprocversion(bekraftaBeslutRequest.getKogitoprocversion())
            .kundbehovsflodeId(UUID.fromString(bekraftaBeslutRequest.getData().getKundbehovsflodeId()))
            .build();
   }
}
