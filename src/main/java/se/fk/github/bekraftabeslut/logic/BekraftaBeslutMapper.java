package se.fk.github.bekraftabeslut.logic;

import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.adapter.arbetsgivare.dto.ArbetsgivareResponse;
import se.fk.rimfrost.adapter.folkbokford.dto.FolkbokfordResponse;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Anstallning;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Ersattning;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Kund;

@ApplicationScoped
public class BekraftaBeslutMapper
{
   public GetDataResponse toBekraftaBeslutResponse(Handlaggning handlaggningsResponse,
         FolkbokfordResponse folkbokfordResponse,
         ArbetsgivareResponse arbetsgivareResponse,
         ObjectMapper objectMapper) throws JsonProcessingException
   {
      var ersattningsList = new ArrayList<Ersattning>();
      var ersattningResult = handlaggningsResponse.yrkande().produceradeResultat().stream()
            .filter(pr -> pr.typ().equalsIgnoreCase("ersattning")).toList();

      for (var yrkandeErsattning : ersattningResult)
      {
         var data = objectMapper.readValue(yrkandeErsattning.data(), se.fk.github.bekraftabeslut.logic.entity.Ersattning.class);

         var ersattning = new Ersattning();
         ersattning.setErsattningId(yrkandeErsattning.id());
         ersattning.setErsattningstyp(data.ersattningstyp());
         ersattning.setOmfattningProcent(data.omfattningProcent());
         ersattning.setBelopp(data.belopp());
         ersattning.setBerakningsgrund(data.berakningsgrund());
         ersattning.setBeslutsutfall(data.beslutsutfall());
         ersattning.setAvslagsanledning(yrkandeErsattning.avslagsanledning());
         ersattning.setFrom(yrkandeErsattning.resultatFrom().toLocalDate());
         ersattning.setTom(yrkandeErsattning.resultatTom().toLocalDate());

         ersattningsList.add(ersattning);
      }

      Kund kund = new Kund();

      if (folkbokfordResponse != null)
      {
         kund.setFornamn(folkbokfordResponse.fornamn());
         kund.setEfternamn(folkbokfordResponse.efternamn());
         kund.setKon(mapKonEnum(folkbokfordResponse.kon()));
      }

      if (arbetsgivareResponse != null)
      {
         Anstallning anstallning = new Anstallning();
         anstallning.setAnstallningsdag(arbetsgivareResponse.anstallningsdag());
         anstallning.setAnstallningsdag(arbetsgivareResponse.anstallningsdag());
         anstallning.setArbetstidProcent(arbetsgivareResponse.arbetstidProcent());
         anstallning.setSistaAnstallningsdag(arbetsgivareResponse.sistaAnstallningsdag());
         anstallning.setOrganisationsnamn(arbetsgivareResponse.organisationsnamn());
         anstallning.setOrganisationsnummer(arbetsgivareResponse.organisationsnummer());

         kund.setAnstallning(anstallning);
      }

      GetDataResponse getDataResponse = new GetDataResponse();
      getDataResponse.handlaggningId(handlaggningsResponse.id());
      getDataResponse.setErsattning(ersattningsList);
      getDataResponse.setKund(kund);

      return getDataResponse;
   }

   private Kund.KonEnum mapKonEnum(FolkbokfordResponse.Kon kon)
   {
      return switch (kon) {
         case MAN -> Kund.KonEnum.MAN;
         case KVINNA -> Kund.KonEnum.KVINNA;
         default -> throw new IllegalStateException("Unexpected value: " + kon);
      };
   }
}
