package se.fk.github.bekraftabeslut.logic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;

import org.eclipse.store.storage.types.StorageManager;
import se.fk.github.bekraftabeslut.logic.dto.GetBekraftaBeslutDataRequest;
import se.fk.github.bekraftabeslut.logic.dto.GetBekraftaBeslutDataResponse;
import se.fk.github.bekraftabeslut.logic.dto.UpdateErsattningDataRequest;
import se.fk.github.bekraftabeslut.storage.BekraftaBeslutDataStorageProvider;
import se.fk.rimfrost.framework.arbetsgivare.adapter.ArbetsgivareAdapter;
import se.fk.rimfrost.framework.arbetsgivare.adapter.dto.ArbetsgivareResponse;
import se.fk.rimfrost.framework.arbetsgivare.adapter.dto.ImmutableArbetsgivareRequest;
import se.fk.rimfrost.framework.folkbokford.adapter.FolkbokfordAdapter;
import se.fk.rimfrost.framework.folkbokford.adapter.dto.FolkbokfordResponse;
import se.fk.rimfrost.framework.folkbokford.adapter.dto.ImmutableFolkbokfordRequest;
import se.fk.rimfrost.framework.kundbehovsflode.adapter.KundbehovsflodeAdapter;
import se.fk.rimfrost.framework.kundbehovsflode.adapter.dto.ImmutableKundbehovsflodeRequest;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.integration.config.RegelConfigProvider;
import se.fk.rimfrost.framework.regel.logic.RegelMapper;
import se.fk.rimfrost.framework.regel.logic.config.RegelConfig;
import se.fk.rimfrost.framework.regel.logic.dto.Beslutsutfall;
import se.fk.rimfrost.framework.regel.logic.entity.ImmutableErsattningData;
import se.fk.rimfrost.framework.regel.logic.entity.ImmutableUnderlag;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceInterface;
import se.fk.rimfrost.framework.regel.manuell.logic.entity.ImmutableRegelData;
import se.fk.rimfrost.framework.regel.manuell.logic.entity.RegelData;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.CommonRegelData;
import se.fk.rimfrost.framework.storage.StorageManagerProvider;

@ApplicationScoped
@Startup
public class BekraftaBeslutService implements RegelManuellServiceInterface
{

   @Inject
   ObjectMapper objectMapper;

   @Inject
   BekraftaBeslutMapper bekraftaBeslutMapper;

   @Inject
   RegelMapper regelMapper;

   @Inject
   RegelConfigProvider regelConfigProvider;

   @Inject
   FolkbokfordAdapter folkbokfordAdapter;

   @Inject
   ArbetsgivareAdapter arbetsgivareAdapter;

   @Inject
   KundbehovsflodeAdapter kundbehovsflodeAdapter;

   @Inject
   BekraftaBeslutDataStorageProvider dataStorageProvider;

   @Inject
   StorageManagerProvider storageManagerProvider;

   StorageManager storageManager;

   CommonRegelData commonRegelData;

   RegelConfig regelConfig;

   @PostConstruct
   public void init()
   {
      regelConfig = regelConfigProvider.getConfig();

      var dataStorage = dataStorageProvider.getDataStorage();
      commonRegelData = dataStorage.getCommonRegelData();

      storageManager = storageManagerProvider.getStorageManager();
   }

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

      RegelData regelData = commonRegelData.getRegelData(request.kundbehovsflodeId());

      updateRegelDataUnderlag(request.kundbehovsflodeId(), regelData, folkbokfordResponse, arbetsgivareResponse);

      // Read RegelData again to get updated version
      regelData = commonRegelData.getRegelData(request.kundbehovsflodeId());

      var putKundbehovsflodeRequest = regelMapper.toPutKundbehovsflodeRequest(request.kundbehovsflodeId(),
            regelData.uppgiftData(), regelData.underlag(), regelConfig);
      kundbehovsflodeAdapter.putKundbehovsflode(putKundbehovsflodeRequest);

      return bekraftaBeslutMapper.toBekraftaBeslutResponse(kundbehovflodesResponse, folkbokfordResponse, arbetsgivareResponse,
            regelData);
   }

   public void updateErsattningData(UpdateErsattningDataRequest updateRequest)
   {
      RegelData regelData = commonRegelData.getRegelData(updateRequest.kundbehovsflodeId());

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

      var updatedRegelData = ImmutableRegelData.builder()
            .from(regelData)
            .ersattningar(updatedList)
            .build();

      synchronized (commonRegelData.getLock())
      {
         var regelDatas = commonRegelData.getRegelDatas();
         regelDatas.put(updateRequest.kundbehovsflodeId(), updatedRegelData);
         storageManager.store(regelDatas);
      }

      var patchKundbehovsflodeRequest = regelMapper.toPatchKundbehovsflodeRequest(updateRequest.kundbehovsflodeId(),
            updatedRegelData.ersattningar());
      kundbehovsflodeAdapter.patchKundbehovsflode(patchKundbehovsflodeRequest);
   }

   private void updateRegelDataUnderlag(UUID kundbehovsflodeId, RegelData regelData, FolkbokfordResponse folkbokfordResponse,
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

      synchronized (commonRegelData.getLock())
      {
         var regelDatas = commonRegelData.getRegelDatas();
         regelDatas.put(kundbehovsflodeId, regelDataBuilder.build());
         storageManager.store(regelDatas);
      }
   }

   @Override
   public Utfall decideUtfall(RegelData regelData)
   {
      return regelData.ersattningar().stream().allMatch(e -> e.beslutsutfall() == Beslutsutfall.JA) ? Utfall.JA : Utfall.NEJ;
   }

   @Override
   public void handleRegelDone(UUID kundbehovsId)
   {
      // Empty since no rule specific data is currently being used
   }
}
