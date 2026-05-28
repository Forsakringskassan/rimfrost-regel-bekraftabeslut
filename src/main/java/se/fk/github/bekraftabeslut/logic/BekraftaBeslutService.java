package se.fk.github.bekraftabeslut.logic;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.github.bekraftabeslut.logic.entity.Beslutsdata;
import se.fk.github.bekraftabeslut.logic.entity.ImmutableBeslutsdata;
import se.fk.rimfrost.framework.regel.manuell.storage.ManuellRegelCommonDataStorage;
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
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.exception.HandlaggningException;
import se.fk.rimfrost.framework.handlaggning.model.*;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.logic.RegelUtils;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellException;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceBase;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceInterface;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.PatchDataRequest;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.UpdateErsattning;

@ApplicationScoped
@Startup
public class BekraftaBeslutService extends RegelManuellServiceBase
      implements RegelManuellServiceInterface<GetDataResponse, PatchDataRequest>
{
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
   ManuellRegelCommonDataStorage dataStorage;

   @Override
   public GetDataResponse readData(Handlaggning handlaggning)
   {
      Yrkande.IndividYrkandeRoll yrkandeIndivid;
      try
      {
         yrkandeIndivid = findYrkandeIndivid(handlaggning.yrkande().individYrkandeRoller()).orElseThrow();
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
            .yrkandeStatus(faststalltYrkandeStatus.id())
            .build();

      var handlaggningUpdate = ImmutableHandlaggningUpdate.builder()
            .from(createHandlaggningUpdate(handlaggning))
            .yrkande(updatedYrkande)
            .build();

      var beviljatId = findBeviljatBeslutsutfallId().orElse(null);

      var beslut = handlaggning.yrkande().beslut();
      var utfall = beviljatId != null
            && beslut != null
            && beslut.beslutsrader().stream().anyMatch(r -> beviljatId.equals(r.beslutsUtfall()))
                  ? Utfall.JA
                  : Utfall.NEJ;

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

   private ProduceratResultat createUpdatedProduceratResultat(Handlaggning handlaggning, UpdateErsattning ersattningUpdate)
   {
      ProduceratResultat ersattningResult;
      try
      {
         ersattningResult = handlaggning.yrkande().produceradeResultat().stream()
               .filter(pr -> pr.id().equals(ersattningUpdate.getErsattningId())).findFirst().orElseThrow();
      }
      catch (NoSuchElementException e)
      {
         logger.error("Failed to locate ProduceratResultat with id {}", ersattningUpdate.getErsattningId(), e);
         throw new RegelManuellException(Response.Status.BAD_REQUEST,
               "Failed to locate ersattning with id " + ersattningUpdate.getErsattningId());
      }

      return ImmutableProduceratResultat.builder()
            .from(ersattningResult)
            .version(ersattningResult.version() + 1)
            .yrkandeStatus(ersattningUpdate.getYrkandestatus())
            .build();
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

   /** Returns the referensdata id for beslutsutfall with kod {@code beviljat}, or empty if not found. */
   private Optional<String> findBeviljatBeslutsutfallId()
   {
      try
      {
         return referensdataAdapter.getBeslutsutfallstyper().stream()
               .filter(r -> r.kod().equalsIgnoreCase("beviljat"))
               .findFirst()
               .map(Referensdata::id);
      }
      catch (ReferensdataException e)
      {
         logger.error("Failed to read beslutsutfallstyp values while searching for beviljat. {}: {}",
               e.getErrorCode(), e.getMessage());
         throw mapToRegelManuellException(e.getErrorCode(), e.getMessage());
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
         logger.error("Failed to write handlaggning information for handlaggning id: {}. {}: {}", handlaggningUpdate.id(),
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
