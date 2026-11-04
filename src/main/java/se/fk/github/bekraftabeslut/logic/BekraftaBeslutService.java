package se.fk.github.bekraftabeslut.logic;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.github.bekraftabeslut.logic.entity.Beslutsdata;
import se.fk.github.bekraftabeslut.logic.entity.ImmutableBeslutsdata;
import se.fk.rimfrost.adapter.arbetsgivare.ArbetsgivareAdapter;
import se.fk.rimfrost.adapter.arbetsgivare.dto.ArbetsgivareResponse;
import se.fk.rimfrost.adapter.arbetsgivare.dto.ImmutableArbetsgivareRequest;
import se.fk.rimfrost.adapter.arbetsgivare.exception.ArbetsgivareErrorCode;
import se.fk.rimfrost.adapter.arbetsgivare.exception.ArbetsgivareException;
import se.fk.rimfrost.adapter.folkbokford.FolkbokfordAdapter;
import se.fk.rimfrost.adapter.folkbokford.FolkbokfordException;
import se.fk.rimfrost.adapter.folkbokford.dto.FolkbokfordResponse;
import se.fk.rimfrost.adapter.folkbokford.dto.ImmutableFolkbokfordRequest;
import se.fk.rimfrost.adapter.referensdata.adapter.ReferensdataAdapter;
import se.fk.rimfrost.adapter.referensdata.adapter.ReferensdataErrorCode;
import se.fk.rimfrost.adapter.referensdata.adapter.ReferensdataException;
import se.fk.rimfrost.adapter.referensdata.model.Referensdata;
import se.fk.rimfrost.ersattningdata.ErsattningData;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.exception.HandlaggningException;
import se.fk.rimfrost.framework.handlaggning.model.*;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.logic.RegelUtils;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellException;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceBase;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceInterface;
import se.fk.rimfrost.framework.regel.manuell.logic.identity.UserIdentityHolder;
import se.fk.rimfrost.framework.regel.oul.logic.OulUppgiftService;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.PatchDataRequest;

@ApplicationScoped
@Startup
public class BekraftaBeslutService extends RegelManuellServiceBase
      implements RegelManuellServiceInterface<GetDataResponse, PatchDataRequest>
{
   public static final String ERSATTNING_OBJEKT_TYP_ID = "ersattning";

   private final Logger logger = LoggerFactory.getLogger(BekraftaBeslutService.class);

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
   OulUppgiftService oulUppgiftService;

   @Inject
   UserIdentityHolder userIdentityHolder;

   @Override
   public GetDataResponse readData(Handlaggning handlaggning)
   {
      RollIYrkande yrkandeIndivid;
      try
      {
         yrkandeIndivid = findYrkandeIndivid(handlaggning.yrkande().rollerIYrkande()).orElseThrow();
      }
      catch (NoSuchElementException e)
      {
         logger.error("Failed to find yrkande individ in handlaggning information. Handlaggning id: {}", handlaggning.id(), e);
         throw new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR,
               "Failed to find yrkande individ in handlaggning information");
      }

      var individ = yrkandeIndivid.individ();

      FolkbokfordResponse folkbokfordResponse = getFolkbokfordInfo(individ.varde(), handlaggning.id());

      ArbetsgivareResponse arbetsgivareResponse = getArbetsgivareInfo(individ.varde(), handlaggning.id());

      return bekraftaBeslutMapper.toBekraftaBeslutResponse(handlaggning, folkbokfordResponse, arbetsgivareResponse,
            objectMapper);
   }

   @Override
   public HandlaggningUpdate updateData(Handlaggning handlaggning, PatchDataRequest request)
   {
      var correlationData = oulUppgiftService.getCorrelationData(handlaggning.id());

      if (correlationData == null)
      {
         logger.error("Failed to find correlation data for id: {}", handlaggning.id());
         throw new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR, "Internal Server Error");
      }

      var ersattningContainersById = handlaggning.yrkande().sakfragorStallningstaganden().stream()
            .filter(c -> c.objektTypId().equalsIgnoreCase(ERSATTNING_OBJEKT_TYP_ID))
            .collect(Collectors.toMap(SakfragaStallningstagandeContainer::id, Function.identity(), (first, second) -> first));

      List<SakfragaStallningstagandeContainer> updatedErsattningContainers = new ArrayList<>();
      List<Uppgiftsdata> underlag = new ArrayList<>();
      List<ErsattningData> updatedErsattningar = new ArrayList<>();
      for (var ersattningUpdate : request.getErsattningar())
      {
         if (ersattningUpdate == null)
         {
            continue;
         }

         var ersattningId = ersattningUpdate.getErsattningId();
         var container = ersattningContainersById.get(ersattningId);

         if (container == null)
         {
            logger.warn("Failed to locate SakfragaStallningstagandeContainer with id {} for handlaggning with id {}",
                  ersattningId, handlaggning.id());
            throw new RegelManuellException(Response.Status.BAD_REQUEST, "Failed to locate ersattning with id " + ersattningId);
         }

         var ersattning = parseErsattning(container);

         underlag.add(RegelUtils.createUppgiftsdatakoppling(ersattning.getId().toString(), ersattning.getVersion()));

         ersattning.setYrkandeStatus(ersattningUpdate.getYrkandestatus());
         ersattning.setVersion(ersattning.getVersion() + 1);

         updatedErsattningar.add(ersattning);

         updatedErsattningContainers.add(
               RegelUtils.createSakfragaStallningstagande(container.id(), container.objektTypId(), ersattning, objectMapper));
      }

      var beslutsdata = ImmutableBeslutsdata.builder()
            .avslutstyp(request.getBeslut().getAvslutstyp())
            .beslutstyp(request.getBeslut().getBeslutstyp())
            .beslutsutfall(request.getBeslut().getBeslutsutfall())
            .build();

      var beslut = createBeslut(
            updatedErsattningar, beslutsdata);

      var updatedYrkande = RegelUtils.createYrkandeWithUpdatedSakfragorStallningstaganden(handlaggning.yrkande(),
            updatedErsattningContainers);

      var updatedYrkandeWithBeslut = ImmutableYrkande.builder()
            .from(updatedYrkande)
            .version(updatedYrkande.version() + 1)
            .addBeslut(beslut)
            .build();

      var updatedUppgift = ImmutableUppgift.builder()
            .from(correlationData.uppgift())
            .addAllUnderlag(underlag)
            .build();

      var updatedHandlaggning = ImmutableHandlaggning.builder()
            .from(handlaggning)
            .yrkande(updatedYrkandeWithBeslut)
            .build();

      logger.info("PATCH HandlaggningUpdate.yrkande version: {}", updatedYrkandeWithBeslut.version());
      return ImmutableHandlaggningUpdate.builder()
            .handlaggning(updatedHandlaggning)
            .uppgift(updatedUppgift)
            .build();
   }

   @Override
   public void done(UUID handlaggningId)
   {
      Handlaggning handlaggning = getHandlaggning(handlaggningId);

      Referensdata faststalltYrkandeStatus;
      try
      {
         faststalltYrkandeStatus = findFaststalltYrkandestatus().orElseThrow();
      }
      catch (NoSuchElementException e)
      {
         logger.error("Failed to find faststallt yrkandestatus in referensdata. Handlaggning id: {}", handlaggningId, e);
         throw new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR,
               "Failed to find faststallt yrkandestatus in referensdata");
      }

      var updatedYrkande = ImmutableYrkande.builder()
            .from(handlaggning.yrkande())
            .version(handlaggning.version() + 1)
            .yrkandeStatus(faststalltYrkandeStatus.id())
            .build();

      var updatedHandlaggning = ImmutableHandlaggning.builder()
            .from(handlaggning)
            .yrkande(updatedYrkande)
            .build();

      var handlaggningUpdate = ImmutableHandlaggningUpdate.builder()
            .from(createHandlaggningUpdate(handlaggning))
            .handlaggning(updatedHandlaggning)
            .build();

      var ersattningResultats = handlaggning.yrkande().sakfragorStallningstaganden().stream()
            .filter(pr -> pr.objektTypId().equalsIgnoreCase(ERSATTNING_OBJEKT_TYP_ID)).toList();

      List<ErsattningData> ersattningar = new ArrayList<>();
      for (var ersattningResultat : ersattningResultats)
      {
         ersattningar.add(getErsattningData(ersattningResultat.data()));
      }

      var utfall = ersattningar.stream().allMatch(e -> e.getBeslutsutfall() == se.fk.rimfrost.ersattningdata.Beslutsutfall.JA)
            ? Utfall.JA
            : Utfall.NEJ;

      logger.info("HandlaggningUpdate.yrkande version: {}", handlaggningUpdate.handlaggning().yrkande().version());
      updateHandlaggning(handlaggningUpdate);

      sendRegelResponse(handlaggningId, utfall);
   }

   public List<se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Referensdata> getYrkandestatus()
   {
      try
      {
         return referensdataAdapter.getYrkandestatusar().stream().map(this::toApiReferensdata).toList();
      }
      catch (ReferensdataException e)
      {
         logger.error("Failed to read yrkandestatus values. {}: {}", e.getErrorCode(), e.getMessage());
         throw mapToRegelManuellException(e.getErrorCode(), e.getMessage());
      }
   }

   public List<se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Referensdata> getAvslutstyp()
   {
      try
      {
         return referensdataAdapter.getAvslutstyper().stream().map(this::toApiReferensdata).toList();
      }
      catch (ReferensdataException e)
      {
         logger.error("Failed to read avslutstyp values. {}: {}", e.getErrorCode(), e.getMessage());
         throw mapToRegelManuellException(e.getErrorCode(), e.getMessage());
      }
   }

   public List<se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Referensdata> getBeslutstyp()
   {
      try
      {
         return referensdataAdapter.getBeslutstyper().stream().map(this::toApiReferensdata).toList();
      }
      catch (ReferensdataException e)
      {
         logger.error("Failed to read beslutstyp values. {}: {}", e.getErrorCode(), e.getMessage());
         throw mapToRegelManuellException(e.getErrorCode(), e.getMessage());
      }
   }

   public List<se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Referensdata> getBeslutsutfallstyp()
   {
      try
      {
         return referensdataAdapter.getBeslutsutfallstyper().stream().map(this::toApiReferensdata).toList();
      }
      catch (ReferensdataException e)
      {
         logger.error("Failed to read beslutsutfall values. {}: {}", e.getErrorCode(), e.getMessage());
         throw mapToRegelManuellException(e.getErrorCode(), e.getMessage());
      }
   }

   private HandlaggningUpdate createHandlaggningUpdate(Handlaggning handlaggning)
   {
      var correlationData = oulUppgiftService.getCorrelationData(handlaggning.id());

      if (correlationData == null)
      {
         logger.error("Failed to read correlation data for handlaggning with id: {}", handlaggning.id());
         throw new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR, "Internal Server Error");
      }

      return ImmutableHandlaggningUpdate.builder()
            .handlaggning(handlaggning)
            .uppgift(correlationData.uppgift())
            .build();
   }

   private Beslut createBeslut(List<ErsattningData> updatedErsattningList, Beslutsdata beslutsdata)
   {
      List<SakfragaStallningstagandeRef> beslutsref = updatedErsattningList.stream()
            .map(this::createSakfragaStallningstagandeRef)
            .toList();

      var beslutsrad = ImmutableBeslutsrad.builder()
            .id(UUID.randomUUID())
            .version(1)
            .avslutsTyp(beslutsdata.avslutstyp())
            .beslutsTyp(beslutsdata.beslutstyp())
            .beslutsUtfall(beslutsdata.beslutsutfall())
            .sakfragorStallningstaganden(beslutsref)
            .build();

      var handlaggareId = userIdentityHolder.getIdentity();

      var beslutsfattare = ImmutableIdtyp.builder() // TODO: Replace with id for handlaggare when available
            .typId(handlaggareId.typId())
            .varde(handlaggareId.varde())
            .build();

      return ImmutableBeslut.builder()
            .id(UUID.randomUUID())
            .version(1)
            .datum(OffsetDateTime.now())
            .beslutsfattare(beslutsfattare)
            .beslutsrader(List.of(beslutsrad))
            .build();
   }

   private Optional<RollIYrkande> findYrkandeIndivid(List<RollIYrkande> individer)
   {
      Map<String, Referensdata> yrkandeRollerMap = null;
      try
      {
         yrkandeRollerMap = referensdataAdapter.getYrkanderoller().stream()
               .collect(Collectors.toMap(Referensdata::id, Function.identity()));
      }
      catch (ReferensdataException e)
      {
         logger.error("Failed to read yrkanderoller values. {}: {}", e.getErrorCode(), e.getMessage());
         throw mapToRegelManuellException(e.getErrorCode(), e.getMessage());
      }

      for (var individYrkandeRoll : individer)
      {
         var roll = yrkandeRollerMap.get(individYrkandeRoll.yrkandeRollId());

         if (roll != null && roll.namn().equalsIgnoreCase("sökande"))
         {
            return Optional.of(individYrkandeRoll);
         }
      }

      return Optional.empty();
   }

   private Optional<Referensdata> findFaststalltYrkandestatus()
   {
      try
      {
         return referensdataAdapter.getYrkandestatusar().stream().filter(r -> r.kod().equalsIgnoreCase("faststallt")).findFirst();
      }
      catch (ReferensdataException e)
      {
         logger.error("Failed to read yrkandestatus values while searching for faststallt yrkandestatus. {}: {}",
               e.getErrorCode(), e.getMessage());
         throw mapToRegelManuellException(e.getErrorCode(), e.getMessage());
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

   private SakfragaStallningstagandeRef createSakfragaStallningstagandeRef(ErsattningData updatedErsattningData)
   {
      return ImmutableSakfragaStallningstagandeRef.builder().id(updatedErsattningData.getId())
            .version(updatedErsattningData.getVersion()).build();
   }

   private ErsattningData parseErsattning(SakfragaStallningstagandeContainer container)
   {
      try
      {
         return ErsattningData.fromJson(container.data(), objectMapper);
      }
      catch (RuntimeException e)
      {
         logger.error("Failed to parse json as ErsattningData", e);
         throw new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR, "Internal Server Error");
      }
   }

   private FolkbokfordResponse getFolkbokfordInfo(String personnummer, UUID handlaggningId)
   {
      var folkbokfordRequest = ImmutableFolkbokfordRequest.builder()
            .personnummer(personnummer)
            .build();

      try
      {
         return folkbokfordAdapter.getFolkbokfordInfo(folkbokfordRequest);
      }
      catch (FolkbokfordException e)
      {
         if (e.getErrorType() == FolkbokfordException.ErrorType.NOT_FOUND)
         {
            return null;
         }

         logger.error(
               "Failed to read folkbokforing response for personnummer: {} for request with handlaggning id: {}. {}: {}",
               personnummer, handlaggningId, e.getErrorType(), e.getMessage());
         throw mapToRegelManuellException(e.getErrorType(), e.getMessage());
      }
   }

   private ArbetsgivareResponse getArbetsgivareInfo(String personnummer, UUID handlaggningId)
   {
      var arbetsgivareRequest = ImmutableArbetsgivareRequest.builder()
            .personnummer(personnummer)
            .build();

      try
      {
         return arbetsgivareAdapter.getArbetsgivareInfo(arbetsgivareRequest);
      }
      catch (ArbetsgivareException e)
      {
         if (e.getErrorCode() == ArbetsgivareErrorCode.NOT_FOUND)
         {
            return null;
         }

         logger.error("Failed to read arbetsgivare response for personnummer: {} for request with handlaggning id: {}. {}: {}",
               personnummer, handlaggningId, e.getErrorCode(), e.getMessage());
         throw mapToRegelManuellException(e.getErrorCode(), e.getMessage());
      }
   }

   private Handlaggning getHandlaggning(UUID handlaggningId)
   {
      try
      {
         return handlaggningAdapter.readHandlaggning(handlaggningId);
      }
      catch (HandlaggningException e)
      {
         logger.error("Failed to read handlaggning information for handlaggning id: {}. {}: {}", handlaggningId, e.getErrorType(),
               e.getMessage());
         throw mapToRegelManuellException(e.getErrorType(), e.getMessage());
      }
   }

   private ErsattningData getErsattningData(String json)
   {
      try
      {
         return ErsattningData.fromJson(json, objectMapper);
      }
      catch (RuntimeException e)
      {
         logger.error("Failed to parse json as ErsattningData", e);
         throw new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR, "Failed to parse json as ErsattningData");
      }
   }

   private void updateHandlaggning(HandlaggningUpdate handlaggningUpdate)
   {
      try
      {
         handlaggningAdapter.updateHandlaggning(handlaggningUpdate);
      }
      catch (HandlaggningException e)
      {
         logger.error("Failed to write handlaggning information for handlaggning id: {}. {}: {}",
               handlaggningUpdate.handlaggning().id(),
               e.getErrorType(), e.getMessage());
         throw mapToRegelManuellException(e.getErrorType(), e.getMessage());
      }
   }

   private RegelManuellException mapToRegelManuellException(FolkbokfordException.ErrorType errorType, String errorMessage)
   {
      return switch (errorType) {
         case BAD_REQUEST -> new RegelManuellException(Response.Status.BAD_REQUEST, errorMessage);
         case SERVICE_UNAVAILABLE ->
                 new RegelManuellException(Response.Status.SERVICE_UNAVAILABLE, errorMessage);
         default ->
                 new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR, errorMessage);
      };
   }

   private RegelManuellException mapToRegelManuellException(ArbetsgivareErrorCode errorCode, String errorMessage)
   {
      return switch (errorCode) {
         case BAD_REQUEST -> new RegelManuellException(Response.Status.BAD_REQUEST, errorMessage);
         case SERVICE_UNAVAILABLE ->
                 new RegelManuellException(Response.Status.SERVICE_UNAVAILABLE, errorMessage);
         default ->
                 new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR, errorMessage);
      };
   }

   private RegelManuellException mapToRegelManuellException(ReferensdataErrorCode errorCode, String errorMessage)
   {
      return switch (errorCode)
      {
         case BAD_REQUEST -> new RegelManuellException(Response.Status.BAD_REQUEST, errorMessage);
         case SERVICE_UNAVAILABLE -> new RegelManuellException(Response.Status.SERVICE_UNAVAILABLE, errorMessage);
         default -> new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR, errorMessage);
      };
   }

   private RegelManuellException mapToRegelManuellException(HandlaggningException.ErrorType errorType, String errorMessage)
   {
      return switch (errorType)
      {
         case BAD_REQUEST -> new RegelManuellException(Response.Status.BAD_REQUEST, errorMessage);
         case SERVICE_UNAVAILABLE -> new RegelManuellException(Response.Status.SERVICE_UNAVAILABLE, errorMessage);
         default -> new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR, errorMessage);
      };
   }
}
