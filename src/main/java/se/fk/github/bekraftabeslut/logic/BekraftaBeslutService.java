package se.fk.github.bekraftabeslut.logic;

import java.util.UUID;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import se.fk.github.bekraftabeslut.integration.arbetsgivare.ArbetsgivareAdapter;
import se.fk.github.bekraftabeslut.integration.arbetsgivare.dto.ArbetsgivareResponse;
import se.fk.github.bekraftabeslut.integration.arbetsgivare.dto.ImmutableArbetsgivareRequest;
import se.fk.github.bekraftabeslut.integration.folkbokford.FolkbokfordAdapter;
import se.fk.github.bekraftabeslut.integration.folkbokford.dto.FolkbokfordResponse;
import se.fk.github.bekraftabeslut.integration.folkbokford.dto.ImmutableFolkbokfordRequest;
import se.fk.github.bekraftabeslut.logic.dto.GetBekraftaBeslutDataRequest;
import se.fk.github.bekraftabeslut.logic.dto.GetBekraftaBeslutDataResponse;
import se.fk.github.bekraftabeslut.logic.dto.UpdateErsattningDataRequest;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.framework.oul.logic.dto.OulResponse;
import se.fk.rimfrost.framework.oul.logic.dto.OulStatus;
import se.fk.rimfrost.framework.oul.presentation.kafka.OulHandlerInterface;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.integration.kundbehovsflode.dto.ImmutableKundbehovsflodeRequest;
import se.fk.rimfrost.framework.regel.logic.dto.Beslutsutfall;
import se.fk.rimfrost.framework.regel.logic.dto.UppgiftStatus;
import se.fk.rimfrost.framework.regel.logic.entity.*;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellService;
import se.fk.rimfrost.framework.oul.presentation.rest.OulUppgiftDoneHandler;

@ApplicationScoped
@Startup
public class BekraftaBeslutService extends RegelManuellService implements OulHandlerInterface, OulUppgiftDoneHandler
{

   @Inject
   ObjectMapper objectMapper;

   @Inject
   BekraftaBeslutMapper bekraftaBeslutMapper;

   @Inject
   FolkbokfordAdapter folkbokfordAdapter;

   @Inject
   ArbetsgivareAdapter arbetsgivareAdapter;

   public GetBekraftaBeslutDataResponse getData(GetBekraftaBeslutDataRequest request) throws JsonProcessingException
   {
      var kundbehovsflodeRequest = ImmutableKundbehovsflodeRequest.builder()
            .kundbehovsflodeId(request.kundbehovsflodeId())
            .build();
      var kundbehovflodesResponse = kundbehovsflodeAdapter.getKundbehovsflodeInfo(kundbehovsflodeRequest);
      var folkbokfordRequest = ImmutableFolkbokfordRequest.builder()
            .personnummer(kundbehovflodesResponse.personnummer())
            .build();
      var folkbokfordResponse = folkbokfordAdapter.getFolkbokfordInfo(folkbokfordRequest);
      var arbetsgivareRequest = ImmutableArbetsgivareRequest.builder()
            .personnummer(kundbehovflodesResponse.personnummer())
            .build();
      var arbetsgivareResponse = arbetsgivareAdapter.getArbetsgivareInfo(arbetsgivareRequest);

      var regelData = regelDatas.get(request.kundbehovsflodeId());

      updateRegelDataUnderlag(regelData, folkbokfordResponse, arbetsgivareResponse);

      updateKundbehovsflodeInfo(regelData);

      return bekraftaBeslutMapper.toBekraftaBeslutResponse(kundbehovflodesResponse, folkbokfordResponse, arbetsgivareResponse,
            regelData);
   }

   @Override
   public void handleOulResponse(OulResponse oulResponse)
   {
      var bekraftaBeslutData = regelDatas.get(oulResponse.kundbehovsflodeId());
      var updatedRegelData = ImmutableRegelData.builder()
            .from(bekraftaBeslutData)
            .uppgiftId(oulResponse.uppgiftId())
            .build();
      regelDatas.put(updatedRegelData.kundbehovsflodeId(), updatedRegelData);
      updateKundbehovsflodeInfo(updatedRegelData);
   }

   public void updateErsattningData(UpdateErsattningDataRequest updateRequest)
   {
      var regelData = regelDatas.get(updateRequest.kundbehovsflodeId());

      var existingErsattning = regelData.ersattningar().stream()
            .filter(e -> e.id().equals(updateRequest.ersattningId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("ErsattningData not found"));

      var updatedErsattning = ImmutableErsattningData.builder()
            .from(existingErsattning)
            .beslutsutfall(updateRequest.beslutsutfall())
            .avslagsanledning(updateRequest.avslagsanledning())
            .build();

      var updatedList = regelData.ersattningar().stream()
            .map(e -> e.id().equals(updateRequest.ersattningId()) ? updatedErsattning : e)
            .toList();

      var updatedBekraftaBeslutData = ImmutableRegelData.builder()
            .from(regelData)
            .ersattningar(updatedList)
            .build();

      regelDatas.put(updateRequest.kundbehovsflodeId(), updatedBekraftaBeslutData);

      updateKundbehovsflodeInfo(updatedBekraftaBeslutData);

   }

   @Override
   public void handleOulStatus(OulStatus oulStatus)
   {
      RegelData regelData = regelDatas.values()
            .stream()
            .filter(r -> r.uppgiftId().equals(oulStatus.uppgiftId()))
            .findFirst()
            .orElse(regelDatas.get(oulStatus.kundbehovsflodeId()));
      updateKundbehovsflodeInfo(regelData);
   }

   private void updateRegelDataUnderlag(RegelData regelData, FolkbokfordResponse folkbokfordResponse,
         ArbetsgivareResponse arbetsgivareResponse) throws JsonProcessingException
   {

      var regelDataBuilder = ImmutableRegelData.builder().from(regelData);

      if (folkbokfordResponse != null)
      {
         var folkbokfordUnderlag = ImmutableUnderlag.builder()
               .typ("FolkbokfördUnderlag")
               .version("1.0")
               .data(objectMapper.writeValueAsString(folkbokfordResponse))
               .build();
         regelDataBuilder.addUnderlag(folkbokfordUnderlag);
      }

      if (arbetsgivareResponse != null)
      {
         var arbetsgivareUnderlag = ImmutableUnderlag.builder()
               .typ("ArbetsgivareUnderlag")
               .version("1.0")
               .data(objectMapper.writeValueAsString(arbetsgivareResponse))
               .build();
         regelDataBuilder.addUnderlag(arbetsgivareUnderlag);
      }

      regelDatas.put(regelData.kundbehovsflodeId(), regelDataBuilder.build());
   }

   private void updateKundbehovsflodeInfo(RegelData regelData)
   {
      var request = bekraftaBeslutMapper.toUpdateKundbehovsflodeRequest(regelData, regelConfigProvider.getConfig());
      kundbehovsflodeAdapter.updateKundbehovsflodeInfo(request);

   }

   @Override
   public void handleUppgiftDone(UUID kundbehovsflodeId)
   {
      var regelData = regelDatas.get(kundbehovsflodeId);

      var updatedRegelDataBuilder = ImmutableRegelData.builder()
            .from(regelData);

      updatedRegelDataBuilder.uppgiftStatus(UppgiftStatus.AVSLUTAD);

      var updatedRegelData = updatedRegelDataBuilder.build();
      regelDatas.put(kundbehovsflodeId, updatedRegelData);

      var utfall = regelData.ersattningar().stream().allMatch(e -> e.beslutsutfall() == Beslutsutfall.JA) ? Utfall.JA
            : Utfall.NEJ;
      var cloudevent = cloudevents.get(updatedRegelData.cloudeventId());
      var regelResponse = regelMapper.toRegelResponse(kundbehovsflodeId, cloudevent, utfall);
      oulKafkaProducer.sendOulStatusUpdate(updatedRegelData.uppgiftId(), Status.AVSLUTAD);
      regelKafkaProducer.sendRegelResponse(regelResponse);

      updateKundbehovsflodeInfo(updatedRegelData);
   }
}
