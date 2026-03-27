package se.fk.github.bekraftabeslut.logic;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import se.fk.github.bekraftabeslut.logic.entity.Ersattning;
import se.fk.github.bekraftabeslut.storage.BekraftaBeslutDataStorage;
import se.fk.rimfrost.framework.arbetsgivare.adapter.ArbetsgivareAdapter;
import se.fk.rimfrost.framework.arbetsgivare.adapter.dto.ImmutableArbetsgivareRequest;
import se.fk.rimfrost.framework.folkbokford.adapter.FolkbokfordAdapter;
import se.fk.rimfrost.framework.folkbokford.adapter.dto.ImmutableFolkbokfordRequest;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.model.*;
import se.fk.rimfrost.framework.individ.adapter.IndividAdapter;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceBase;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceInterface;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.ImmutableManuellRegelCommonData;
import se.fk.rimfrost.framework.yrkanderoll.adapter.YrkanderollAdapter;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Beslutsutfall;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.PatchDataRequest;

@ApplicationScoped
@Startup
public class BekraftaBeslutService extends RegelManuellServiceBase
      implements RegelManuellServiceInterface<GetDataResponse, PatchDataRequest>
{
   @Inject
   BekraftaBeslutMapper bekraftaBeslutMapper;

   @Inject
   IndividAdapter individAdapter;

   @Inject
   FolkbokfordAdapter folkbokfordAdapter;

   @Inject
   ArbetsgivareAdapter arbetsgivareAdapter;

   @Inject
   HandlaggningAdapter handlaggningAdapter;

   @Inject
   YrkanderollAdapter yrkanderollAdapter;

   @Inject
   BekraftaBeslutDataStorage dataStorage;

   @Override
   public GetDataResponse readData(Handlaggning handlaggning)
   {
      var yrkandeIndivid = findYrkandeIndivid(handlaggning.yrkande().individYrkandeRoller()).orElseThrow();

      var individ = individAdapter.getIndivid(yrkandeIndivid.individId());

      var folkbokfordRequest = ImmutableFolkbokfordRequest.builder()
            .personnummer(individ.varde())
            .build();
      var folkbokfordResponse = folkbokfordAdapter.getFolkbokfordInfo(folkbokfordRequest);
      var arbetsgivareRequest = ImmutableArbetsgivareRequest.builder()
            .personnummer(individ.varde())
            .build();
      var arbetsgivareResponse = arbetsgivareAdapter.getArbetsgivareInfo(arbetsgivareRequest);

      try
      {
         return bekraftaBeslutMapper.toBekraftaBeslutResponse(handlaggning, folkbokfordResponse, arbetsgivareResponse,
               objectMapper);
      }
      catch (JsonProcessingException e)
      {
         throw new RuntimeException(e);
      }
   }

   @Override
   public HandlaggningUpdate updateData(Handlaggning handlaggning, PatchDataRequest request)
   {
      var handlaggningUpdate = createHandlaggningUpdate(handlaggning);
      var ersattningResult = handlaggningUpdate.yrkande().produceradeResultat().stream()
            .filter(pr -> pr.id().equals(request.getErsattningId())).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("ErsattningData not found"));

      var updatedErsattningResult = ImmutableProduceratResultat.builder()
            .from(ersattningResult)
            .version(ersattningResult.version() + 1)
            .yrkandeStatus(mapYrkandestatus(request.getYrkandestatus()))
            .build();

      var updatedYrkande = ImmutableYrkande.builder()
            .from(handlaggningUpdate.yrkande())
            .addProduceradeResultat(updatedErsattningResult)
            .build();

      var updatedHandlaggningUpdate = ImmutableHandlaggningUpdate.builder()
            .from(handlaggningUpdate)
            .yrkande(updatedYrkande)
            .build();

      var commonData = dataStorage.getManuellRegelCommonData(updatedHandlaggningUpdate.id());
      var updatedCommonData = ImmutableManuellRegelCommonData.builder()
            .from(commonData)
            .handlaggningUpdate(updatedHandlaggningUpdate)
            .build();
      dataStorage.setManuellRegelCommonData(updatedHandlaggningUpdate.id(), updatedCommonData);

      return updatedHandlaggningUpdate;
   }

   @Override
   public void done(UUID handlaggningId)
   {
      var handlaggning = handlaggningAdapter.readHandlaggning(handlaggningId);
      var ersattningResultats = handlaggning.yrkande().produceradeResultat().stream()
            .filter(pr -> pr.typ().equalsIgnoreCase("ersattning")).toList();

      List<Ersattning> ersattningar = new ArrayList<>();
      for (var ersattningResultat : ersattningResultats)
      {
         ersattningar.add(getErsattning(ersattningResultat).orElseThrow());
      }

      var utfall = ersattningar.stream().allMatch(e -> e.beslutsutfall() == Beslutsutfall.JA) ? Utfall.JA : Utfall.NEJ;
      sendRegelResponse(handlaggningId, utfall);
   }

   private HandlaggningUpdate createHandlaggningUpdate(Handlaggning handlaggning)
   {
      var commonData = dataStorage.getManuellRegelCommonData(handlaggning.id());

      return ImmutableHandlaggningUpdate.builder()
            .id(handlaggning.id())
            .version(handlaggning.version())
            .yrkande(handlaggning.yrkande())
            .processInstansId(handlaggning.processInstansId())
            .skapadTS(handlaggning.skapadTS())
            .avslutadTS(handlaggning.avslutadTS())
            .handlaggningspecifikationId(handlaggning.handlaggningspecifikationId())
            .uppgift(commonData.uppgift())
            .build();
   }

   private Yrkandestatus mapYrkandestatus(se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Yrkandestatus ersattningstatus)
   {
      return switch (ersattningstatus) {
         case PLANERAT -> Yrkandestatus.PLANERAT;
         case YRKAT -> Yrkandestatus.YRKAT;
         case FASTSTALLT -> Yrkandestatus.FASTSTALLT;
         case UNDER_UTREDNING -> Yrkandestatus.UNDER_UTREDNING;
         case FASTSTALLT_UNDER_UTREDNING -> Yrkandestatus.FASTSTALLT_UNDER_UTREDNING;
         default -> throw new IllegalStateException("Unexpected value: " + ersattningstatus);
      };
   }

   private Optional<Yrkande.IndividYrkandeRoll> findYrkandeIndivid(List<Yrkande.IndividYrkandeRoll> individer)
   {
      for (var individYrkandeRoll : individer)
      {
         var roll = yrkanderollAdapter.getYrkanderoll(individYrkandeRoll.yrkandeRollId());

         if (roll != null && roll.namn().equalsIgnoreCase("sökande"))
         {
            return Optional.of(individYrkandeRoll);
         }
      }

      return Optional.empty();
   }

   private Optional<Ersattning> getErsattning(ProduceratResultat produceratResultat)
   {
      try
      {
         return Optional.of(objectMapper.readValue(produceratResultat.data(), Ersattning.class));
      }
      catch (JsonProcessingException e)
      {
         return Optional.empty();
      }
   }
}
