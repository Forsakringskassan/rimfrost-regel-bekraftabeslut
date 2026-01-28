package se.fk.github.bekraftabeslut.integration.arbetsgivare;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.bekraftabeslut.integration.arbetsgivare.dto.ArbetsgivareResponse;
import se.fk.github.bekraftabeslut.integration.arbetsgivare.dto.ImmutableArbetsgivareResponse;
import se.fk.rimfrost.api.arbetsgivare.jaxrsspec.controllers.generatedsource.model.GetArbetsgivare200Response;

@ApplicationScoped
public class ArbetsgivareMapper
{

   public ArbetsgivareResponse toArbetsgivareResponse(GetArbetsgivare200Response apiResponse)
   {
      var anstallning = apiResponse.getAnstallningar().getFirst();

      return ImmutableArbetsgivareResponse.builder()
            .organisationsnamn(anstallning.getOrganisation().getNamn())
            .organisationsnummer(anstallning.getOrganisation().getNummer())
            .anstallningsdag(anstallning.getStartdag())
            .arbetstidProcent(anstallning.getArbetstid())
            .lonFrom(anstallning.getStartdag())
            .loneSumma(40000)
            .build();
   }
}
