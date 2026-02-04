package se.fk.github.bekraftabeslut.presentation.rest;

import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.bekraftabeslut.logic.dto.GetBekraftaBeslutDataResponse;
import se.fk.github.bekraftabeslut.logic.dto.ImmutableUpdateErsattningDataRequest;
import se.fk.github.bekraftabeslut.logic.dto.UpdateErsattningDataRequest;
import se.fk.rimfrost.framework.regel.logic.dto.Beslutsutfall;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Anstallning;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Ersattning;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Kund;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Lon;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.PatchDataRequest;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Kund.KonEnum;

@ApplicationScoped
public class BekraftaBeslutRestMapper
{

   public GetDataResponse toGetDataResponse(GetBekraftaBeslutDataResponse bekraftaBeslutResponse)
   {
      var lon = new Lon();
      lon.setFrom(bekraftaBeslutResponse.lonFrom());
      lon.setTom(bekraftaBeslutResponse.lonTom());
      lon.setLonesumma(bekraftaBeslutResponse.loneSumma());

      var anstallning = new Anstallning();
      anstallning.setAnstallningsdag(bekraftaBeslutResponse.anstallningsdag());
      anstallning.setArbetstidProcent(bekraftaBeslutResponse.arbetstidProcent());
      anstallning.setSistaAnstallningsdag(bekraftaBeslutResponse.sistaAnstallningsdag());
      anstallning.setOrganisationsnamn(bekraftaBeslutResponse.organisationsnamn());
      anstallning.setOrganisationsnummer(bekraftaBeslutResponse.organisationsnummer());
      anstallning.setLon(lon);

      var kund = new Kund();
      kund.setFornamn(bekraftaBeslutResponse.fornamn());
      kund.setEfternamn(bekraftaBeslutResponse.efternamn());
      kund.setAnstallning(anstallning);
      kund.setKon(mapKonEnum(bekraftaBeslutResponse.kon()));

      var response = new GetDataResponse();
      response.setKund(kund);
      response.kundbehovsflodeId(bekraftaBeslutResponse.kundbehovsflodeId());
      for (var bekraftaBeslutErsattning : bekraftaBeslutResponse.ersattning())
      {
         var ersattning = new Ersattning();
         ersattning.setBelopp(bekraftaBeslutErsattning.belopp());
         ersattning.setBerakningsgrund(bekraftaBeslutErsattning.berakningsgrund());
         ersattning.setErsattningId(bekraftaBeslutErsattning.ersattningsId());
         ersattning.setErsattningstyp(bekraftaBeslutErsattning.ersattningsTyp());
         ersattning.setOmfattningProcent(bekraftaBeslutErsattning.omfattningsProcent());
         ersattning.setFrom(bekraftaBeslutErsattning.from());
         ersattning.setTom(bekraftaBeslutErsattning.tom());
         ersattning.setAvslagsanledning(bekraftaBeslutErsattning.avslagsanledning());
         if (bekraftaBeslutErsattning.beslutsutfall() != null)
         {
            ersattning.setBeslutsutfall(mapBeslutsutfall(bekraftaBeslutErsattning.beslutsutfall()));
         }
         response.addErsattningItem(ersattning);
      }
      return response;
   }

   public UpdateErsattningDataRequest toUpdateErsattningDataRequest(UUID kundbehovsflodeId, PatchDataRequest patchRequest)
   {
      return ImmutableUpdateErsattningDataRequest.builder()
            .kundbehovsflodeId(kundbehovsflodeId)
            .beslutsutfall(mapBeslutsutfall(patchRequest.getBeslutsutfall()))
            .ersattningId(patchRequest.getErsattningId())
            .avslagsanledning(patchRequest.getAvslagsanledning())
            .signerad(patchRequest.getSignera())
            .build();
   }

   private se.fk.rimfrost.framework.regel.logic.dto.Beslutsutfall mapBeslutsutfall(
         se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Beslutsutfall beslututfall)
   {
       return switch (beslututfall) {
           case JA -> Beslutsutfall.JA;
           case NEJ -> Beslutsutfall.NEJ;
           case FU -> Beslutsutfall.FU;
           default -> null;
       };
   }

   private se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Beslutsutfall mapBeslutsutfall(
         se.fk.rimfrost.framework.regel.logic.dto.Beslutsutfall beslututfall)
   {
       return switch (beslututfall) {
           case JA ->
                   se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Beslutsutfall.JA;
           case NEJ ->
                   se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Beslutsutfall.NEJ;
           case FU ->
                   se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Beslutsutfall.FU;
           default -> null;
       };
   }

   private KonEnum mapKonEnum(String kon)
   {
      return switch(kon){case"Man"->KonEnum.MAN;default->KonEnum.KVINNA;};
   }

}
