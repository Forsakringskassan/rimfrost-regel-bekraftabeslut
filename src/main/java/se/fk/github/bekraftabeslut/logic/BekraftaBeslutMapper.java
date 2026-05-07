package se.fk.github.bekraftabeslut.logic;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.adapter.arbetsgivare.dto.ArbetsgivareResponse;
import se.fk.rimfrost.adapter.folkbokford.dto.FolkbokfordResponse;
import se.fk.rimfrost.ersattningdata.ErsattningData;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellException;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Anstallning;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Beslutsutfall;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Ersattning;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Kund;

@ApplicationScoped
public class BekraftaBeslutMapper
{
   Logger logger = LoggerFactory.getLogger(BekraftaBeslutMapper.class);

   public GetDataResponse toBekraftaBeslutResponse(Handlaggning handlaggningsResponse,
         FolkbokfordResponse folkbokfordResponse,
         ArbetsgivareResponse arbetsgivareResponse,
         ObjectMapper objectMapper)
   {
      var ersattningsList = new ArrayList<Ersattning>();
      var ersattningResult = handlaggningsResponse.yrkande().produceradeResultat().stream()
            .filter(pr -> pr.typ().equalsIgnoreCase("ersattning")).toList();

      for (var yrkandeErsattning : ersattningResult)
      {
         ErsattningData data = null;
         try
         {
            data = ErsattningData.fromJson(yrkandeErsattning.data(), objectMapper);
         }
         catch (RuntimeException e)
         {
            logger.error("Failed to parse json as ErsattningData", e);
            throw new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR, "Failed to parse json as ErsattningData");
         }

         var ersattning = new Ersattning();
         ersattning.setErsattningId(yrkandeErsattning.id());
         ersattning.setErsattningstyp(data.getErsattningstyp().id());
         ersattning.setOmfattningProcent(data.getOmfattningProcent());
         ersattning.setBelopp(data.getBelopp());
         ersattning.setBerakningsgrund(data.getBerakningsgrund());
         ersattning.setBeslutsutfall(mapBeslutsutfallEnum(data.getBeslutsutfall()));
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

   private Beslutsutfall mapBeslutsutfallEnum(se.fk.rimfrost.ersattningdata.Beslutsutfall beslutsutfall)
   {
      return switch(beslutsutfall) {
         case JA -> Beslutsutfall.JA;
         case NEJ -> Beslutsutfall.NEJ;
         case FU -> Beslutsutfall.FU;
         default -> throw new IllegalStateException("Unexpected value: " + beslutsutfall);
      };
   }
}
