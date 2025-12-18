package se.fk.github.bekraftabeslut.integration.kundbehovsflode;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.ImmutableErsattning;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.ImmutableKundbehovsflodeResponse;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.KundbehovsflodeResponse;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.UpdateKundbehovsflodeRequest;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.GetKundbehovsflodeResponse;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.PutKundbehovsflodeRequest;

@ApplicationScoped
public class KundbehovsflodeMapper
{

   public KundbehovsflodeResponse toKundbehovsflodeResponse(GetKundbehovsflodeResponse apiResponse)
   {
      var responseBuilder = ImmutableKundbehovsflodeResponse.builder()
            .personnummer(apiResponse.getKundbehovsflode().getKundbehov().getKundbehovsroll().getFirst().getIndivid().getId())
            .kundbehovsflodeId(apiResponse.getKundbehovsflode().getId());

      for (var ersattning : apiResponse.getKundbehovsflode().getKundbehov().getErsattning())
      {
         responseBuilder.addErsattning(ImmutableErsattning.builder()
               .belopp(ersattning.getBelopp())
               .berakningsgrund(ersattning.getBerakningsgrund().ordinal())
               .ersattningsId(ersattning.getId())
               .ersattningsTyp(ersattning.getErsattningstyp().toString())
               .franOchMed(ersattning.getProduceratResultat().getFirst().getFrom().toLocalDate())
               .tillOchMed(ersattning.getProduceratResultat().getFirst().getTom().toLocalDate())
               .omfattningsProcent(ersattning.getOmfattning())
               .build());
      }
      return responseBuilder.build();
   }

   public PutKundbehovsflodeRequest toApiRequest(UpdateKundbehovsflodeRequest request, GetKundbehovsflodeResponse apiResponse)
   {
      var putRequest = new PutKundbehovsflodeRequest();
      var kundbehovflode = apiResponse.getKundbehovsflode();
      var ersattningar = apiResponse.getKundbehovsflode().getKundbehov().getErsattning();

      for (var ersattning : request.ersattningar())
      {
         var ersattningItem = ersattningar.stream().filter(e -> e.getId().equals(ersattning.id())).findFirst().get();
         ersattningItem.setAvslagsanledning(ersattning.avslagsanledning() == null ? "" : ersattning.avslagsanledning());
         ersattningItem.setBeslutsutfall(ersattning.beslutsutfall());
      }
      var kundbehov = kundbehovflode.getKundbehov();
      kundbehov.setErsattning(ersattningar);
      kundbehovflode.setKundbehov(kundbehov);
      putRequest.setKundbehovsflode(kundbehovflode);
      return putRequest;
   }
}
