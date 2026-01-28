package se.fk.github.bekraftabeslut.logic.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.immutables.value.Value;
import jakarta.annotation.Nullable;

@Value.Immutable
public interface GetBekraftaBeslutDataResponse
{

   UUID kundbehovsflodeId();

   String fornamn();

   String efternamn();

   String kon();

   String organisationsnummer();

   String organisationsnamn();

   int arbetstidProcent();

   LocalDate anstallningsdag();

   @Nullable
   LocalDate sistaAnstallningsdag();

   int loneSumma();

   LocalDate lonFrom();

   @Nullable
   LocalDate lonTom();

   List<Ersattning> ersattning();

   @Value.Immutable
   public interface Ersattning
   {

      UUID ersattningsId();

      String ersattningsTyp();

      int omfattningsProcent();

      int belopp();

      int berakningsgrund();

      @Nullable
      Beslutsutfall beslutsutfall();

      LocalDate from();

      LocalDate tom();

      @Nullable
      String avslagsanledning();
   }
}
