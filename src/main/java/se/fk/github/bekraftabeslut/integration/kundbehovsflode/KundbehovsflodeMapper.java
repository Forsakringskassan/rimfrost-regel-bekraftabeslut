package se.fk.github.bekraftabeslut.integration.kundbehovsflode;

import java.util.ArrayList;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.ImmutableErsattning;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.ImmutableKundbehovsflodeResponse;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.KundbehovsflodeResponse;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.UpdateKundbehovsflodeRequest;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.*;

import static se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.UppgiftStatus.PLANERAD;

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
               .franOchMed(ersattning.getProduceratResultat().getFrom().toLocalDate())
               .tillOchMed(ersattning.getProduceratResultat().getTom().toLocalDate())
               .omfattningsProcent(ersattning.getOmfattning())
               .build());
      }
      return responseBuilder.build();
   }

   public PutKundbehovsflodeRequest toApiRequest(UpdateKundbehovsflodeRequest request, GetKundbehovsflodeResponse apiResponse)
   {
      var putRequest = new PutKundbehovsflodeRequest();

      var lagrum = new Lagrum();
      lagrum.setId(request.uppgift().specifikation().regel().lagrum().id());
      lagrum.setVersion(request.uppgift().specifikation().regel().lagrum().version());
      lagrum.setForfattning(request.uppgift().specifikation().regel().lagrum().forfattning());
      lagrum.setGiltigFrom(request.uppgift().specifikation().regel().lagrum().giltigFrom());
      lagrum.setGiltigTom(request.uppgift().specifikation().regel().lagrum().giltigTom());
      lagrum.setKapitel(request.uppgift().specifikation().regel().lagrum().kapitel());
      lagrum.setParagraf(request.uppgift().specifikation().regel().lagrum().paragraf());
      lagrum.setPunkt(request.uppgift().specifikation().regel().lagrum().punkt());
      lagrum.setStycke(request.uppgift().specifikation().regel().lagrum().stycke());

      var regel = new Regel();
      regel.setId(request.uppgift().specifikation().regel().id());
      regel.setVersion(request.uppgift().specifikation().regel().version());
      regel.setLagrum(lagrum);

      var uppgiftspecifikation = new Uppgiftspecifikation();
      uppgiftspecifikation.setId(UUID.randomUUID());
      uppgiftspecifikation.setApplikationsId("bekraftabeslut");
      uppgiftspecifikation.setApplikationsVersion("1.0");
      uppgiftspecifikation.setNamn("Rätt till försäkring - manuell kontroll");
      uppgiftspecifikation.setRoll(Roll.ANSVARIG_HANDLAGGARE);
      uppgiftspecifikation.setUppgiftbeskrivning("Kontrollera om personen varit på jobbet");
      uppgiftspecifikation.setUppgiftsGui("bekraftabeslut/" + request.kundbehovsflodeId().toString());
      uppgiftspecifikation.setVerksamhetslogik(Verksamhetslogik.A);
      uppgiftspecifikation.setVersion("1.0");
      uppgiftspecifikation.setRegel(regel);

      var underlagList = new ArrayList<Underlag>();
      for (var underlag : request.underlag())
      {
         var underlagitem = new Underlag();
         underlagitem.typ(underlag.typ());
         underlagitem.version(underlag.version());
         underlagitem.data(underlag.data());
         underlagList.add(underlagitem);
      }

      var uppgift = new Uppgift();

      uppgift.setId(request.uppgift().id());
      uppgift.setFsSAinformation(request.uppgift().fsSAinformation());
      uppgift.setSkapadTs(request.uppgift().skapadTs());
      uppgift.setUtfordTs(request.uppgift().utfordTs());
      uppgift.setUppgiftStatus(mapUppgiftStatus(request.uppgift().uppgiftStatus()));
      uppgift.setUtforarId(request.uppgift().utforarId());
      uppgift.setVersion(request.uppgift().version());
      uppgift.setUppgiftspecifikation(uppgiftspecifikation);
      uppgift.setUnderlag(underlagList);

      var kundbehovflode = apiResponse.getKundbehovsflode();
      var ersattningar = apiResponse.getKundbehovsflode().getKundbehov().getErsattning();

      for (var ersattning : request.ersattningar())
      {
         var ersattningItem = ersattningar.stream().filter(e -> e.getId().equals(ersattning.id())).findFirst().get();
         ersattningItem.setAvslagsanledning(ersattning.avslagsanledning() == null ? "" : ersattning.avslagsanledning());
         ersattningItem.setBeslutsutfall(ersattning.beslutsutfall());
      }

      uppgift.setUnderlag(underlagList);

      var kundbehov = kundbehovflode.getKundbehov();
      kundbehov.setErsattning(ersattningar);
      kundbehovflode.setKundbehov(kundbehov);
      uppgift.setKundbehovsflode(kundbehovflode);
      putRequest.setUppgift(uppgift);
      return putRequest;
   }

   private UppgiftStatus mapUppgiftStatus(
         se.fk.github.bekraftabeslut.logic.dto.UppgiftStatus uppgiftStatus)
   {
      switch (uppgiftStatus)
      {
         case TILLDELAD:
            return UppgiftStatus.TILLDELAD;
         case AVSLUTAD:
            return UppgiftStatus.AVSLUTAD;
         case PLANERAD:
            return PLANERAD;
         default:
            throw new InternalError("Could not map UppgiftStatus: " + uppgiftStatus);
      }
   }
}
