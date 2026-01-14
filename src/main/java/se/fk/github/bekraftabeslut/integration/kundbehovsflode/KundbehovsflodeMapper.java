package se.fk.github.bekraftabeslut.integration.kundbehovsflode;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.ImmutableErsattning;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.ImmutableKundbehovsflodeResponse;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.KundbehovsflodeResponse;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.UpdateKundbehovsflodeRequest;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.FSSAinformation;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.GetKundbehovsflodeResponse;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.PutKundbehovsflodeRequest;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Regel;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Roll;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Underlag;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Uppgift;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.UppgiftStatus;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Uppgiftspecifikation;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Verksamhetslogik;

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
      var uppgift = new Uppgift();

      //Vart ska hämta denna uppgiftdata från?
      uppgift.setId(request.uppgiftId());
      uppgift.setFsSAinformation(FSSAinformation.HANDLAGGNING_PAGAR);
      uppgift.setSkapadTs(OffsetDateTime.now());
      uppgift.setUtfordTs(OffsetDateTime.now());
      uppgift.setUppgiftStatus(UppgiftStatus.AVSLUTAD);
      uppgift.setUtforarId(UUID.randomUUID());
      uppgift.setVersion("1.0");

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
      uppgiftspecifikation.setRegel(new ArrayList<Regel>());
      uppgift.setUppgiftspecifikation(uppgiftspecifikation);

      var kundbehovflode = apiResponse.getKundbehovsflode();
      var ersattningar = apiResponse.getKundbehovsflode().getKundbehov().getErsattning();

      for (var ersattning : request.ersattningar())
      {
         var ersattningItem = ersattningar.stream().filter(e -> e.getId().equals(ersattning.id())).findFirst().get();
         ersattningItem.setAvslagsanledning(ersattning.avslagsanledning() == null ? "" : ersattning.avslagsanledning());
         ersattningItem.setBeslutsutfall(ersattning.beslutsutfall());
      }

      var underlagList = new ArrayList<Underlag>();
      for (var underlag : request.underlag())
      {
         var underlagitem = new Underlag();
         underlagitem.typ(underlag.typ());
         underlagitem.version(underlag.version());
         underlagitem.data(underlag.data());
         underlagList.add(underlagitem);
      }

      uppgift.setUnderlag(underlagList);

      var kundbehov = kundbehovflode.getKundbehov();
      kundbehov.setErsattning(ersattningar);
      kundbehovflode.setKundbehov(kundbehov);
      uppgift.setKundbehovsflode(kundbehovflode);
      putRequest.setUppgift(uppgift);
      return putRequest;
   }
}
