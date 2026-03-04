package se.fk.github.bekraftabeslut.logic;

import java.util.ArrayList;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.bekraftabeslut.logic.dto.ImmutableErsattning;
import se.fk.github.bekraftabeslut.logic.dto.ImmutableGetBekraftaBeslutDataResponse;
import se.fk.github.bekraftabeslut.logic.dto.GetBekraftaBeslutDataResponse;
import se.fk.github.bekraftabeslut.logic.dto.GetBekraftaBeslutDataResponse.Ersattning;
import se.fk.rimfrost.framework.arbetsgivare.adapter.dto.ArbetsgivareResponse;
import se.fk.rimfrost.framework.folkbokford.adapter.dto.FolkbokfordResponse;
import se.fk.rimfrost.framework.handlaggning.adapter.dto.HandlaggningResponse;
import se.fk.rimfrost.framework.regel.logic.entity.ErsattningData;
import se.fk.rimfrost.framework.regel.manuell.logic.entity.RegelData;

@ApplicationScoped
public class BekraftaBeslutMapper
{

   public GetBekraftaBeslutDataResponse toBekraftaBeslutResponse(HandlaggningResponse handlaggningsResponse,
         FolkbokfordResponse folkbokfordResponse,
         ArbetsgivareResponse arbetsgivareResponse,
         RegelData regelData)
   {
      var ersattningsList = new ArrayList<Ersattning>();

      for (var yrkandeErsattning : handlaggningsResponse.ersattning())
      {
         ErsattningData bekraftaBeslutErsattning = regelData.ersattningar().stream()
               .filter(e -> e.id().equals(yrkandeErsattning.ersattningsId()))
               .findFirst()
               .orElseThrow(() -> new IllegalArgumentException("ErsattningData not found"));

         var ersattning = ImmutableErsattning.builder()
               .belopp(yrkandeErsattning.belopp())
               .berakningsgrund(yrkandeErsattning.berakningsgrund())
               .ersattningsId(yrkandeErsattning.ersattningsId())
               .ersattningsTyp(yrkandeErsattning.ersattningsTyp())
               .from(yrkandeErsattning.franOchMed())
               .tom(yrkandeErsattning.tillOchMed())
               .avslagsanledning(bekraftaBeslutErsattning.avslagsanledning())
               .omfattningsProcent(yrkandeErsattning.omfattningsProcent());

         if (bekraftaBeslutErsattning.beslutsutfall() != null)
         {
            ersattning.beslutsutfall(bekraftaBeslutErsattning.beslutsutfall());
         }

         ersattningsList.add(ersattning.build());
      }

      var builder = ImmutableGetBekraftaBeslutDataResponse.builder()
            .handlaggningId(handlaggningsResponse.handlaggningId())
            .ersattning(ersattningsList);

      if (folkbokfordResponse != null)
      {
         builder
               .fornamn(folkbokfordResponse.fornamn())
               .efternamn(folkbokfordResponse.efternamn())
               .kon(folkbokfordResponse.kon().toString());
      }

      if (arbetsgivareResponse != null)
      {
         builder
               .anstallningsdag(arbetsgivareResponse.anstallningsdag())
               .sistaAnstallningsdag(arbetsgivareResponse.sistaAnstallningsdag())
               .arbetstidProcent(arbetsgivareResponse.arbetstidProcent())
               .loneSumma(40000) //TODO: Replace when salary is available in api response
               .lonFrom(arbetsgivareResponse.anstallningsdag()) // TODO: Replace when salary start date is available in api response
               .lonTom(arbetsgivareResponse.anstallningsdag()) // TODO: Replace when salary end date is available in api response
               .organisationsnamn(arbetsgivareResponse.organisationsnamn())
               .organisationsnummer(arbetsgivareResponse.organisationsnummer());
      }
      return builder.build();
   }

}
