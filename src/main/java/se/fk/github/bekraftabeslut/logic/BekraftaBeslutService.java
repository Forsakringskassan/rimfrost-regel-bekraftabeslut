package se.fk.github.bekraftabeslut.logic;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import se.fk.github.bekraftabeslut.logic.entity.Ersattning;
import se.fk.github.bekraftabeslut.storage.BekraftaBeslutDataStorage;
import se.fk.rimfrost.adapter.arbetsgivare.ArbetsgivareAdapter;
import se.fk.rimfrost.adapter.arbetsgivare.dto.ImmutableArbetsgivareRequest;
import se.fk.rimfrost.adapter.folkbokford.FolkbokfordAdapter;
import se.fk.rimfrost.adapter.folkbokford.dto.ImmutableFolkbokfordRequest;
import se.fk.rimfrost.adapter.individ.adapter.IndividAdapter;
import se.fk.rimfrost.adapter.yrkanderoll.adapter.YrkanderollAdapter;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.model.*;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.logic.RegelUtils;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceBase;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceInterface;
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

      var updatedYrkande = RegelUtils.createYrkandeWithUpdatedProduceradeResultat(handlaggningUpdate.yrkande(),
            List.of(updatedErsattningResult));

      return ImmutableHandlaggningUpdate.builder()
            .from(handlaggningUpdate)
            .yrkande(updatedYrkande)
            .build();
   }

   @Override
   public void done(UUID handlaggningId)
   {
      var handlaggning = handlaggningAdapter.readHandlaggning(handlaggningId);

      var beslut = createBeslut(handlaggning.yrkande().produceradeResultat());

      var updatedYrkande = ImmutableYrkande.builder()
            .from(handlaggning.yrkande())
            .beslut(beslut)
            .build();

      var handlaggningUpdate = ImmutableHandlaggningUpdate.builder()
            .from(createHandlaggningUpdate(handlaggning))
            .yrkande(updatedYrkande)
            .build();

      var ersattningResultats = handlaggning.yrkande().produceradeResultat().stream()
            .filter(pr -> pr.typ().equalsIgnoreCase("ersattning")).toList();

      List<Ersattning> ersattningar = new ArrayList<>();
      for (var ersattningResultat : ersattningResultats)
      {
         ersattningar.add(getErsattning(ersattningResultat).orElseThrow());
      }

      var utfall = ersattningar.stream().allMatch(e -> e.beslutsutfall() == Beslutsutfall.JA) ? Utfall.JA : Utfall.NEJ;

      handlaggningAdapter.updateHandlaggning(handlaggningUpdate);
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

   private Beslut createBeslut(List<ProduceratResultat> produceratResultat)
   {
      List<ProduceratResultatRef> beslutsref = produceratResultat.stream()
            .map(pr -> (ProduceratResultatRef) ImmutableProduceratResultatRef.builder().id(pr.id()).version(pr.version()).build())
            .toList();

      var beslutsrad = ImmutableBeslutsrad.builder()
            .id(UUID.randomUUID())
            .version(1)
            .avslutsTyp(UUID.randomUUID()) // TODO: Set to correct value when available
            .beslutsTyp(UUID.randomUUID()) // TODO: Set to correct value when available
            .beslutsUtfall(UUID.randomUUID()) // TODO: Set to correct value when available
            .produceradeResultatRef(beslutsref)
            .build();

      return ImmutableBeslut.builder()
            .id(UUID.randomUUID())
            .version(1)
            .datum(OffsetDateTime.now())
            .beslutsfattare(UUID.randomUUID()) // TODO: Set to id for handlaggare when available
            .beslutsrader(List.of(beslutsrad))
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
