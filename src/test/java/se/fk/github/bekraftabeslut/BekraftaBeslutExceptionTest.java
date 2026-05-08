package se.fk.github.bekraftabeslut;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import se.fk.github.bekraftabeslut.logic.BekraftaBeslutService;
import se.fk.github.bekraftabeslut.storage.BekraftaBeslutDataStorage;
import se.fk.rimfrost.adapter.arbetsgivare.ArbetsgivareAdapter;
import se.fk.rimfrost.adapter.arbetsgivare.dto.ArbetsgivareResponse;
import se.fk.rimfrost.adapter.arbetsgivare.exception.ArbetsgivareErrorCode;
import se.fk.rimfrost.adapter.arbetsgivare.exception.ArbetsgivareException;
import se.fk.rimfrost.adapter.folkbokford.FolkbokfordAdapter;
import se.fk.rimfrost.adapter.folkbokford.FolkbokfordException;
import se.fk.rimfrost.adapter.folkbokford.dto.FolkbokfordResponse;
import se.fk.rimfrost.adapter.referensdata.adapter.ReferensdataAdapter;
import se.fk.rimfrost.adapter.referensdata.adapter.ReferensdataErrorCode;
import se.fk.rimfrost.adapter.referensdata.adapter.ReferensdataException;
import se.fk.rimfrost.adapter.referensdata.model.ImmutableReferensdata;
import se.fk.rimfrost.adapter.referensdata.model.Referensdata;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.exception.HandlaggningException;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableHandlaggning;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableYrkande;
import se.fk.rimfrost.framework.handlaggning.model.ProduceratResultat;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import se.fk.rimfrost.framework.handlaggning.model.Yrkande;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellException;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.ManuellRegelCommonData;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static se.fk.github.bekraftabeslut.BekraftaBeslutTestData.newPatchDataRequest;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockBekraftaBeslut.class)
})
public class BekraftaBeslutExceptionTest
{
   private static final String yrkandeRollId = "1234";

   @InjectMock
   ArbetsgivareAdapter arbetsgivareAdapter;

   @InjectMock
   FolkbokfordAdapter folkbokfordAdapter;

   @InjectMock
   ReferensdataAdapter referensdataAdapter;

   @InjectMock
   HandlaggningAdapter handlaggningAdapter;

   @InjectMock
   BekraftaBeslutDataStorage bekraftaBeslutDataStorage;

   @Inject
   BekraftaBeslutService bekraftaBeslutService;

   @ParameterizedTest
   @EnumSource(value = FolkbokfordException.ErrorType.class, names =
   {
         "NOT_FOUND"
   }, mode = EnumSource.Mode.EXCLUDE)
   void folkbokford_exception_maps_to_regel_manuell_exception(FolkbokfordException.ErrorType errorType)
         throws FolkbokfordException, ReferensdataException
   {
      when(referensdataAdapter.getYrkanderoller()).thenReturn(createYrkandeRoller());
      when(folkbokfordAdapter.getFolkbokfordInfo(any()))
            .thenThrow(new FolkbokfordException(errorType, "test"));

      var exception = assertThrows(RegelManuellException.class,
            () -> bekraftaBeslutService.readData(handlaggningMock()));

      assertEquals(expectedStatus(errorType), exception.getStatus());
   }

   @ParameterizedTest
   @EnumSource(value = ArbetsgivareErrorCode.class, names =
   {
         "NOT_FOUND"
   }, mode = EnumSource.Mode.EXCLUDE)
   void arbetsgivare_exception_maps_to_regel_manuell_exception(ArbetsgivareErrorCode errorCode)
         throws FolkbokfordException, ArbetsgivareException, ReferensdataException
   {
      when(referensdataAdapter.getYrkanderoller()).thenReturn(createYrkandeRoller());
      when(folkbokfordAdapter.getFolkbokfordInfo(any()))
            .thenReturn(mock(FolkbokfordResponse.class));
      when(arbetsgivareAdapter.getArbetsgivareInfo(any()))
            .thenThrow(new ArbetsgivareException("test", errorCode));

      var exception = assertThrows(RegelManuellException.class,
            () -> bekraftaBeslutService.readData(handlaggningMock()));

      assertEquals(expectedStatus(errorCode), exception.getStatus());
   }

   @Test
   void invalid_ersattningdata_maps_to_regel_manuell_exception()
         throws ReferensdataException, FolkbokfordException, ArbetsgivareException
   {
      var produceratResultat = mock(ProduceratResultat.class, Mockito.RETURNS_DEEP_STUBS);
      when(produceratResultat.data()).thenReturn("(:?");
      when(produceratResultat.typ()).thenReturn("ersattning");

      var handlaggning = handlaggningMock();
      when(handlaggning.yrkande().produceradeResultat()).thenReturn(List.of(produceratResultat));

      when(referensdataAdapter.getYrkanderoller()).thenReturn(createYrkandeRoller());
      when(folkbokfordAdapter.getFolkbokfordInfo(any()))
            .thenReturn(mock(FolkbokfordResponse.class));
      when(arbetsgivareAdapter.getArbetsgivareInfo(any()))
            .thenReturn(mock(ArbetsgivareResponse.class));

      var exception = assertThrows(RegelManuellException.class,
            () -> bekraftaBeslutService.readData(handlaggning));

      assertEquals(Response.Status.INTERNAL_SERVER_ERROR, exception.getStatus());
   }

   @ParameterizedTest
   @EnumSource(value = HandlaggningException.ErrorType.class)
   void handlaggning_read_exception_maps_to_regel_manuell_exception(HandlaggningException.ErrorType errorType)
         throws HandlaggningException
   {
      when(handlaggningAdapter.readHandlaggning(any()))
            .thenThrow(new HandlaggningException(errorType, "test"));

      var exception = assertThrows(RegelManuellException.class,
            () -> bekraftaBeslutService.done(UUID.randomUUID()));

      assertEquals(expectedStatus(errorType), exception.getStatus());
   }

   @ParameterizedTest
   @EnumSource(value = ReferensdataErrorCode.class)
   void yrkandestatus_exception_maps_to_regel_manuell_exception_in_done(ReferensdataErrorCode errorCode)
         throws HandlaggningException, ReferensdataException
   {
      var handlaggning = handlaggningMock();
      when(handlaggningAdapter.readHandlaggning(Mockito.any())).thenReturn(handlaggning);
      when(referensdataAdapter.getYrkandestatusar()).thenThrow(new ReferensdataException(errorCode, "test"));

      var exception = assertThrows(RegelManuellException.class,
            () -> bekraftaBeslutService.done(UUID.randomUUID()));

      assertEquals(expectedStatus(errorCode), exception.getStatus());
   }

   @Test
   void invalid_ersattningdata_maps_to_regel_manuell_exception_in_done() throws HandlaggningException, ReferensdataException
   {
      var produceratResultat = mock(ProduceratResultat.class, Mockito.RETURNS_DEEP_STUBS);
      when(produceratResultat.data()).thenReturn("(:?");
      when(produceratResultat.typ()).thenReturn("ersattning");

      var handlaggning = createHandlaggning();
      var yrkande = ImmutableYrkande.builder()
            .from(handlaggning.yrkande())
            .produceradeResultat(List.of(produceratResultat))
            .build();
      var updatedHandlaggning = ImmutableHandlaggning.builder()
            .from(handlaggning)
            .yrkande(yrkande)
            .build();

      var commonData = mock(ManuellRegelCommonData.class);
      when(commonData.uppgift()).thenReturn(mock(Uppgift.class));

      when(handlaggningAdapter.readHandlaggning(Mockito.any())).thenReturn(updatedHandlaggning);
      when(referensdataAdapter.getYrkandestatusar()).thenReturn(createYrkandestatusar());
      when(bekraftaBeslutDataStorage.getManuellRegelCommonData(Mockito.any())).thenReturn(commonData);

      var exception = assertThrows(RegelManuellException.class,
            () -> bekraftaBeslutService.done(UUID.randomUUID()));

      assertEquals(Response.Status.INTERNAL_SERVER_ERROR, exception.getStatus());
   }

   @ParameterizedTest
   @EnumSource(value = HandlaggningException.ErrorType.class)
   void handlaggning_write_exception_maps_to_regel_manuell_exception(HandlaggningException.ErrorType errorType)
         throws HandlaggningException, ReferensdataException
   {
      var commonData = mock(ManuellRegelCommonData.class);
      when(commonData.uppgift()).thenReturn(mock(Uppgift.class));

      when(handlaggningAdapter.readHandlaggning(Mockito.any())).thenReturn(createHandlaggning());
      when(handlaggningAdapter.updateHandlaggning(Mockito.any())).thenThrow(new HandlaggningException(errorType, "test"));
      when(referensdataAdapter.getYrkandestatusar()).thenReturn(createYrkandestatusar());
      when(bekraftaBeslutDataStorage.getManuellRegelCommonData(Mockito.any())).thenReturn(commonData);

      var exception = assertThrows(RegelManuellException.class,
            () -> bekraftaBeslutService.done(UUID.randomUUID()));

      assertEquals(expectedStatus(errorType), exception.getStatus());
   }

   @ParameterizedTest
   @EnumSource(value = ReferensdataErrorCode.class)
   void yrkandestatus_exception_maps_to_regel_manuell_exception(ReferensdataErrorCode errorCode)
         throws HandlaggningException, ReferensdataException
   {
      var handlaggning = handlaggningMock();
      when(handlaggningAdapter.readHandlaggning(Mockito.any())).thenReturn(handlaggning);
      when(referensdataAdapter.getYrkandestatusar()).thenThrow(new ReferensdataException(errorCode, "test"));

      var exception = assertThrows(RegelManuellException.class,
            () -> bekraftaBeslutService.getYrkandestatus());

      assertEquals(expectedStatus(errorCode), exception.getStatus());
   }

   @ParameterizedTest
   @EnumSource(value = ReferensdataErrorCode.class)
   void avslutstyp_exception_maps_to_regel_manuell_exception(ReferensdataErrorCode errorCode)
         throws HandlaggningException, ReferensdataException
   {
      var handlaggning = handlaggningMock();
      when(handlaggningAdapter.readHandlaggning(Mockito.any())).thenReturn(handlaggning);
      when(referensdataAdapter.getAvslutstyper()).thenThrow(new ReferensdataException(errorCode, "test"));

      var exception = assertThrows(RegelManuellException.class,
            () -> bekraftaBeslutService.getAvslutstyp());

      assertEquals(expectedStatus(errorCode), exception.getStatus());
   }

   @ParameterizedTest
   @EnumSource(value = ReferensdataErrorCode.class)
   void beslutstyp_exception_maps_to_regel_manuell_exception(ReferensdataErrorCode errorCode)
         throws HandlaggningException, ReferensdataException
   {
      var handlaggning = handlaggningMock();
      when(handlaggningAdapter.readHandlaggning(Mockito.any())).thenReturn(handlaggning);
      when(referensdataAdapter.getBeslutstyper()).thenThrow(new ReferensdataException(errorCode, "test"));

      var exception = assertThrows(RegelManuellException.class,
            () -> bekraftaBeslutService.getBeslutstyp());

      assertEquals(expectedStatus(errorCode), exception.getStatus());
   }

   @ParameterizedTest
   @EnumSource(value = ReferensdataErrorCode.class)
   void beslutsutfallstyp_exception_maps_to_regel_manuell_exception(ReferensdataErrorCode errorCode)
         throws HandlaggningException, ReferensdataException
   {
      var handlaggning = handlaggningMock();
      when(handlaggningAdapter.readHandlaggning(Mockito.any())).thenReturn(handlaggning);
      when(referensdataAdapter.getBeslutsutfallstyper()).thenThrow(new ReferensdataException(errorCode, "test"));

      var exception = assertThrows(RegelManuellException.class,
            () -> bekraftaBeslutService.getBeslutsutfallstyp());

      assertEquals(expectedStatus(errorCode), exception.getStatus());
   }

   @Test
   void ersattning_not_found_exception_maps_to_regel_manuell_exception()
   {
      var commonData = mock(ManuellRegelCommonData.class);
      when(commonData.uppgift()).thenReturn(mock(Uppgift.class));
      when(bekraftaBeslutDataStorage.getManuellRegelCommonData(Mockito.any())).thenReturn(commonData);

      var exception = assertThrows(RegelManuellException.class,
            () -> bekraftaBeslutService.updateData(createHandlaggning(), newPatchDataRequest()));

      assertEquals(Response.Status.BAD_REQUEST, exception.getStatus());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "(?i)^Failed to find yrkande individ in handlaggning information.*"
   })
   void yrkande_individ_not_found_exception_maps_to_regel_manuell_exception(String expectedMsgRegex) throws ReferensdataException
   {
      when(referensdataAdapter.getYrkanderoller()).thenReturn(List.of());

      var exception = assertThrows(RegelManuellException.class,
            () -> bekraftaBeslutService.readData(handlaggningMock()));

      assertEquals(Response.Status.INTERNAL_SERVER_ERROR, exception.getStatus());
      assertTrue(exception.getMessage().matches(expectedMsgRegex));
   }

   @ParameterizedTest
   @CsvSource(
   {
         "f674a6ab-ffcd-4a28-a748-dce0e1b2e20d, (?i)^Failed to find faststallt yrkandestatus in referensdata.*"
   })
   void faststallt_yrkandestatus_not_found_exception_maps_to_regel_manuell_exception(UUID handlaggningId, String expectedMsgRegex)
         throws ReferensdataException, HandlaggningException
   {
      when(handlaggningAdapter.readHandlaggning(handlaggningId)).thenReturn(mock(Handlaggning.class));
      when(referensdataAdapter.getYrkandestatusar()).thenReturn(List.of());

      var exception = assertThrows(RegelManuellException.class,
            () -> bekraftaBeslutService.done(handlaggningId));

      assertEquals(Response.Status.INTERNAL_SERVER_ERROR, exception.getStatus());
      assertTrue(exception.getMessage().matches(expectedMsgRegex));
   }

   private static Response.Status expectedStatus(FolkbokfordException.ErrorType errorType)
    {
        return switch (errorType)
        {
            case NOT_FOUND -> Response.Status.INTERNAL_SERVER_ERROR;
            case BAD_REQUEST -> Response.Status.BAD_REQUEST;
            case SERVICE_UNAVAILABLE -> Response.Status.SERVICE_UNAVAILABLE;
            case UNEXPECTED_ERROR -> Response.Status.INTERNAL_SERVER_ERROR;
        };
    }

   private static Response.Status expectedStatus(ArbetsgivareErrorCode errorCode)
    {
        return switch (errorCode)
        {
            case NOT_FOUND -> Response.Status.INTERNAL_SERVER_ERROR;
            case BAD_REQUEST -> Response.Status.BAD_REQUEST;
            case SERVICE_UNAVAILABLE -> Response.Status.SERVICE_UNAVAILABLE;
            case UNEXPECTED_ERROR -> Response.Status.INTERNAL_SERVER_ERROR;
        };
    }

   private static Response.Status expectedStatus(HandlaggningException.ErrorType errorCode)
    {
        return switch (errorCode)
        {
            case NOT_FOUND -> Response.Status.INTERNAL_SERVER_ERROR;
            case BAD_REQUEST -> Response.Status.BAD_REQUEST;
            case SERVICE_UNAVAILABLE -> Response.Status.SERVICE_UNAVAILABLE;
            case UNEXPECTED_ERROR -> Response.Status.INTERNAL_SERVER_ERROR;
        };
    }

   private static Response.Status expectedStatus(ReferensdataErrorCode errorCode)
    {
        return switch (errorCode)
        {
            case NOT_FOUND -> Response.Status.INTERNAL_SERVER_ERROR;
            case BAD_REQUEST -> Response.Status.BAD_REQUEST;
            case SERVICE_UNAVAILABLE -> Response.Status.SERVICE_UNAVAILABLE;
            case UNEXPECTED_ERROR -> Response.Status.INTERNAL_SERVER_ERROR;
        };
    }

   private static Handlaggning handlaggningMock()
   {
      var individYrkandeRoll = mock(Yrkande.IndividYrkandeRoll.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
      var handlaggning = mock(Handlaggning.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
      when(handlaggning.yrkande().individYrkandeRoller())
            .thenReturn(List.of(individYrkandeRoll));
      when(individYrkandeRoll.individ().varde()).thenReturn("19901010-1234");
      when(individYrkandeRoll.yrkandeRollId())
            .thenReturn(yrkandeRollId);
      return handlaggning;
   }

   private static Handlaggning createHandlaggning()
   {
      var yrkande = ImmutableYrkande.builder()
            .id(UUID.randomUUID())
            .version(1)
            .erbjudandeId(UUID.randomUUID().toString())
            .yrkandeDatum(OffsetDateTime.now())
            .yrkandeStatus("faststallt")
            .yrkandeFrom(OffsetDateTime.now())
            .yrkandeTom(OffsetDateTime.now())
            .avsikt("avsikt")
            .individYrkandeRoller(List.of())
            .produceradeResultat(List.of())
            .beslut(null)
            .build();

      return ImmutableHandlaggning.builder()
            .id(UUID.randomUUID())
            .version(1)
            .yrkande(yrkande)
            .processInstansId(UUID.randomUUID())
            .skapadTS(OffsetDateTime.now())
            .handlaggningspecifikationId(UUID.randomUUID())
            .build();
   }

   private static List<Referensdata> createYrkandeRoller()
   {
      var referensdata = ImmutableReferensdata.builder()
            .id(yrkandeRollId)
            .namn("sökande")
            .kod("abc-1234")
            .build();
      return List.of(referensdata);
   }

   private static List<Referensdata> createYrkandestatusar()
   {
      var referensdata = ImmutableReferensdata.builder()
            .id("1234")
            .kod("faststallt")
            .namn("Faststallt")
            .build();
      return List.of(referensdata);
   }
}
