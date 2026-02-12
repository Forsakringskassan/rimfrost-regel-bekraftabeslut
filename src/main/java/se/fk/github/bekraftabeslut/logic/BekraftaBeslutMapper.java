package se.fk.github.bekraftabeslut.logic;

import java.util.ArrayList;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.bekraftabeslut.logic.dto.ImmutableErsattning;
import se.fk.github.bekraftabeslut.logic.dto.ImmutableGetBekraftaBeslutDataResponse;
import se.fk.github.bekraftabeslut.logic.dto.GetBekraftaBeslutDataResponse;
import se.fk.github.bekraftabeslut.logic.dto.GetBekraftaBeslutDataResponse.Ersattning;
import se.fk.rimfrost.framework.arbetsgivare.adapter.dto.ArbetsgivareResponse;
import se.fk.rimfrost.framework.folkbokford.adapter.dto.FolkbokfordResponse;
import se.fk.rimfrost.framework.kundbehovsflode.adapter.dto.KundbehovsflodeResponse;
import se.fk.rimfrost.framework.regel.logic.entity.ErsattningData;
import se.fk.rimfrost.framework.regel.logic.entity.RegelData;

@ApplicationScoped
public class BekraftaBeslutMapper
{

   public GetBekraftaBeslutDataResponse toBekraftaBeslutResponse(KundbehovsflodeResponse kundbehovflodesResponse,
         FolkbokfordResponse folkbokfordResponse,
         ArbetsgivareResponse arbetsgivareResponse,
         RegelData regelData)
   {
      var ersattningsList = new ArrayList<Ersattning>();

      for (var kundbehovErsattning : kundbehovflodesResponse.ersattning())
      {
         ErsattningData bekraftaBeslutErsattning = regelData.ersattningar().stream()
               .filter(e -> e.id().equals(kundbehovErsattning.ersattningsId()))
               .findFirst()
               .orElseThrow(() -> new IllegalArgumentException("ErsattningData not found"));

         var ersattning = ImmutableErsattning.builder()
               .belopp(kundbehovErsattning.belopp())
               .berakningsgrund(kundbehovErsattning.berakningsgrund())
               .ersattningsId(kundbehovErsattning.ersattningsId())
               .ersattningsTyp(kundbehovErsattning.ersattningsTyp())
               .from(kundbehovErsattning.franOchMed())
               .tom(kundbehovErsattning.tillOchMed())
               .avslagsanledning(bekraftaBeslutErsattning.avslagsanledning())
               .omfattningsProcent(kundbehovErsattning.omfattningsProcent());

         if (bekraftaBeslutErsattning.beslutsutfall() != null)
         {
            ersattning.beslutsutfall(bekraftaBeslutErsattning.beslutsutfall());
         }

         ersattningsList.add(ersattning.build());
      }

      var builder = ImmutableGetBekraftaBeslutDataResponse.builder()
            .kundbehovsflodeId(kundbehovflodesResponse.kundbehovsflodeId())
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
