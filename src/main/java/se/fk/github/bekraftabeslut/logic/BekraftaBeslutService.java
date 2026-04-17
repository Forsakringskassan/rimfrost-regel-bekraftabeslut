package se.fk.github.bekraftabeslut.logic;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import se.fk.github.bekraftabeslut.logic.entity.Beslutsdata;
import se.fk.github.bekraftabeslut.logic.entity.Ersattning;
import se.fk.github.bekraftabeslut.logic.entity.ImmutableBeslutsdata;
import se.fk.github.bekraftabeslut.storage.BekraftaBeslutDataStorage;
import se.fk.rimfrost.adapter.arbetsgivare.ArbetsgivareAdapter;
import se.fk.rimfrost.adapter.arbetsgivare.dto.ImmutableArbetsgivareRequest;
import se.fk.rimfrost.adapter.folkbokford.FolkbokfordAdapter;
import se.fk.rimfrost.adapter.folkbokford.dto.ImmutableFolkbokfordRequest;
import se.fk.rimfrost.adapter.referensdata.adapter.ReferensdataAdapter;
import se.fk.rimfrost.adapter.referensdata.model.Referensdata;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.model.*;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.logic.RegelUtils;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceBase;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceInterface;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Beslutsutfall;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.PatchDataRequest;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.UpdateErsattning;

@ApplicationScoped
@Startup
public class BekraftaBeslutService extends RegelManuellServiceBase
      implements RegelManuellServiceInterface<GetDataResponse, PatchDataRequest>
{
   @Inject
   BekraftaBeslutMapper bekraftaBeslutMapper;

   @Inject
   FolkbokfordAdapter folkbokfordAdapter;

   @Inject
   ArbetsgivareAdapter arbetsgivareAdapter;

   @Inject
   HandlaggningAdapter handlaggningAdapter;

   @Inject
   ReferensdataAdapter referensdataAdapter;

   @Inject
   BekraftaBeslutDataStorage dataStorage;

   @Override
   public GetDataResponse readData(Handlaggning handlaggning)
   {
      var yrkandeIndivid = findYrkandeIndivid(handlaggning.yrkande().individYrkandeRoller()).orElseThrow();

      var individ = yrkandeIndivid.individ();

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
      var updatedErsattningar = request.getErsattningar().stream().map(e -> createUpdatedProduceratResultat(handlaggning, e))
            .toList();

      var beslutsdata = ImmutableBeslutsdata.builder()
            .avslutstyp(request.getBeslut().getAvslutstyp())
            .beslutstyp(request.getBeslut().getBeslutstyp())
            .beslutsutfall(request.getBeslut().getBeslutsutfall())
            .build();

      var beslut = createBeslut(handlaggning.id(), updatedErsattningar, beslutsdata);

      var updatedYrkande = RegelUtils.createYrkandeWithUpdatedProduceradeResultat(handlaggningUpdate.yrkande(),
            updatedErsattningar);

      var updatedYrkandeWithBeslut = ImmutableYrkande.builder()
            .from(updatedYrkande)
            .beslut(beslut)
            .build();

      return ImmutableHandlaggningUpdate.builder()
            .from(handlaggningUpdate)
            .yrkande(updatedYrkandeWithBeslut)
            .build();
   }

   @Override
   public void done(UUID handlaggningId)
   {
      var handlaggning = handlaggningAdapter.readHandlaggning(handlaggningId);
      var faststalltYrkandeStatus = findFaststalltYrkandeStatus().orElseThrow();

      var updatedYrkande = ImmutableYrkande.builder()
            .from(handlaggning.yrkande())
            .yrkandeStatus(faststalltYrkandeStatus.id())
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

   public List<se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Referensdata> getYrkandestatus()
   {
      return referensdataAdapter.getYrkandestatusar().stream().map(this::toApiReferensdata).toList();
   }

   public List<se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Referensdata> getAvslutstyp()
   {
      return referensdataAdapter.getAvslutstyper().stream().map(this::toApiReferensdata).toList();
   }

   public List<se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Referensdata> getBeslutstyp()
   {
      return referensdataAdapter.getBeslutstyper().stream().map(this::toApiReferensdata).toList();
   }

   public List<se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Referensdata> getBeslutsutfallstyp()
   {
      return referensdataAdapter.getBeslutsutfallstyper().stream().map(this::toApiReferensdata).toList();
   }

   private HandlaggningUpdate createHandlaggningUpdate(Handlaggning handlaggning)
   {
      var commonData = dataStorage.getManuellRegelCommonData(handlaggning.id());

      return ImmutableHandlaggningUpdate.builder()
            .id(handlaggning.id())
            .version(handlaggning.version())
            .yrkande(handlaggning.yrkande())
            .processInstansId(Objects.requireNonNull(handlaggning.processInstansId()))
            .skapadTS(handlaggning.skapadTS())
            .avslutadTS(handlaggning.avslutadTS())
            .handlaggningspecifikationId(handlaggning.handlaggningspecifikationId())
            .uppgift(commonData.uppgift())
            .build();
   }

   private Beslut createBeslut(UUID handlaggningsId, List<ProduceratResultat> produceratResultat, Beslutsdata beslutsdata)
   {
      List<ProduceratResultatRef> beslutsref = produceratResultat.stream()
            .map(pr -> (ProduceratResultatRef) ImmutableProduceratResultatRef.builder().id(pr.id()).version(pr.version()).build())
            .toList();

      var beslutsrad = ImmutableBeslutsrad.builder()
            .id(UUID.randomUUID())
            .version(1)
            .avslutsTyp(beslutsdata.avslutstyp())
            .beslutsTyp(beslutsdata.beslutstyp())
            .beslutsUtfall(beslutsdata.beslutsutfall())
            .produceradeResultatRef(beslutsref)
            .build();

      var beslutsfattare = ImmutableIdtyp.builder() // TODO: Replace with id for handlaggare when available
            .typId(UUID.randomUUID().toString())
            .varde(UUID.randomUUID().toString())
            .build();

      return ImmutableBeslut.builder()
            .id(UUID.randomUUID())
            .version(1)
            .datum(OffsetDateTime.now())
            .beslutsfattare(beslutsfattare)
            .beslutsrader(List.of(beslutsrad))
            .build();
   }

   private Optional<Yrkande.IndividYrkandeRoll> findYrkandeIndivid(List<Yrkande.IndividYrkandeRoll> individer)
   {
      for (var individYrkandeRoll : individer)
      {
         var roll = referensdataAdapter.getYrkanderoll(individYrkandeRoll.yrkandeRollId());

         if (roll != null && roll.namn().equalsIgnoreCase("sökande"))
         {
            return Optional.of(individYrkandeRoll);
         }
      }

      return Optional.empty();
   }

   private Optional<Referensdata> findFaststalltYrkandeStatus()
   {
      return referensdataAdapter.getYrkandestatusar().stream().filter(r -> r.kod().equalsIgnoreCase("faststallt")).findFirst();
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

   private se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Referensdata toApiReferensdata(
         Referensdata referensdata)
   {
      se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Referensdata apiReferensdata = new se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Referensdata();
      apiReferensdata.setId(referensdata.id());
      apiReferensdata.setKod(referensdata.kod());
      apiReferensdata.setNamn(referensdata.namn());

      return apiReferensdata;
   }

   private ProduceratResultat createUpdatedProduceratResultat(Handlaggning handlaggning, UpdateErsattning ersattningUpdate)
   {
      var ersattningResult = handlaggning.yrkande().produceradeResultat().stream()
            .filter(pr -> pr.id().equals(ersattningUpdate.getErsattningId())).findFirst().orElseThrow();

      return ImmutableProduceratResultat.builder()
            .from(ersattningResult)
            .version(ersattningResult.version() + 1)
            .yrkandeStatus(ersattningUpdate.getYrkandestatus())
            .build();
   }
}
