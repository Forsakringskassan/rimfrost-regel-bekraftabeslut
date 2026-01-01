package se.fk.github.bekraftabeslut.logic;

import java.util.ArrayList;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.bekraftabeslut.integration.arbetsgivare.dto.ArbetsgivareResponse;
import se.fk.github.bekraftabeslut.integration.folkbokford.dto.FolkbokfordResponse;
import se.fk.github.bekraftabeslut.integration.kafka.dto.ImmutableBekraftaBeslutResponseRequest;
import se.fk.github.bekraftabeslut.integration.kafka.dto.BekraftaBeslutResponseRequest;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.ImmutableUpdateKundbehovsflodeErsattning;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.ImmutableUpdateKundbehovsflodeRequest;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.ImmutableUpdateKundbehovsflodeUnderlag;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.KundbehovsflodeResponse;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.UpdateKundbehovsflodeRequest;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.UpdateKundbehovsflodeUnderlag;
import se.fk.github.bekraftabeslut.logic.dto.ImmutableErsattning;
import se.fk.github.bekraftabeslut.logic.dto.ImmutableGetBekraftaBeslutDataResponse;
import se.fk.github.bekraftabeslut.logic.dto.Beslutsutfall;
import se.fk.github.bekraftabeslut.logic.dto.GetBekraftaBeslutDataResponse;
import se.fk.github.bekraftabeslut.logic.dto.GetBekraftaBeslutDataResponse.Ersattning;
import se.fk.github.bekraftabeslut.logic.entity.CloudEventData;
import se.fk.github.bekraftabeslut.logic.entity.ErsattningData;
import se.fk.github.bekraftabeslut.logic.entity.BekraftaBeslutData;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Ersattning.BeslutsutfallEnum;

@ApplicationScoped
public class BekraftaBeslutMapper
{

   public GetBekraftaBeslutDataResponse toBekraftaBeslutResponse(KundbehovsflodeResponse kundbehovflodesResponse,
         FolkbokfordResponse folkbokfordResponse, ArbetsgivareResponse arbetsgivareResponse, BekraftaBeslutData bekraftaBeslutData)
   {
      var ersattningsList = new ArrayList<Ersattning>();

      for (var kundbehovErsattning : kundbehovflodesResponse.ersattning())
      {
         ErsattningData bekraftaBeslutErsattning = bekraftaBeslutData.ersattningar().stream()
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
               .loneSumma(arbetsgivareResponse.loneSumma())
               .lonFrom(arbetsgivareResponse.lonFrom())
               .lonTom(arbetsgivareResponse.lonTom())
               .organisationsnamn(arbetsgivareResponse.organisationsnamn())
               .organisationsnummer(arbetsgivareResponse.organisationsnummer());
      }
      return builder.build();
   }

   public BekraftaBeslutResponseRequest toBekraftaBeslutResponseRequest(BekraftaBeslutData bekraftaBeslutData, CloudEventData cloudevent, boolean rattTillForsakring)
   {
      return ImmutableBekraftaBeslutResponseRequest.builder()
            .id(cloudevent.id())
            .kundbehovsflodeId(bekraftaBeslutData.kundbehovsflodeId())
            .kogitoparentprociid(cloudevent.kogitoparentprociid())
            .kogitorootprociid(cloudevent.kogitorootprociid())
            .kogitoprocid(cloudevent.kogitoprocid())
            .kogitorootprocid(cloudevent.kogitorootprocid())
            .kogitoprocinstanceid(cloudevent.kogitoprocinstanceid())
            .kogitoprocist(cloudevent.kogitoprocist())
            .kogitoprocversion(cloudevent.kogitoprocversion())
            .rattTillForsakring(rattTillForsakring)
            .build();
   }

   public UpdateKundbehovsflodeRequest toUpdateKundbehovsflodeRequest(BekraftaBeslutData bekraftaBeslutData)
   {

      var requestBuilder = ImmutableUpdateKundbehovsflodeRequest.builder()
            .kundbehovsflodeId(bekraftaBeslutData.kundbehovsflodeId())
            .underlag(new ArrayList<UpdateKundbehovsflodeUnderlag>())
            .uppgiftId(bekraftaBeslutData.uppgiftId());

      for (ErsattningData bekraftaBeslutErsattning : bekraftaBeslutData.ersattningar())
      {
         var ersattning = ImmutableUpdateKundbehovsflodeErsattning.builder()
               .beslutsutfall(mapBeslutsutfall(bekraftaBeslutErsattning.beslutsutfall()))
               .id(bekraftaBeslutErsattning.id())
               .avslagsanledning(bekraftaBeslutErsattning.avslagsanledning())
               .build();
         requestBuilder.addErsattningar(ersattning);
      }

      for (var bekraftaBeslutUnderlag : bekraftaBeslutData.underlag())
      {
         var underlag = ImmutableUpdateKundbehovsflodeUnderlag.builder()
               .typ(bekraftaBeslutUnderlag.typ())
               .version(bekraftaBeslutUnderlag.version())
               .data(bekraftaBeslutUnderlag.data())
               .build();
         requestBuilder.addUnderlag(underlag);
      }

      return requestBuilder.build();
   }

   private BeslutsutfallEnum mapBeslutsutfall(
         Beslutsutfall beslutsutfall)
   {
      if (beslutsutfall == null)
      {
         return BeslutsutfallEnum.FU;
      }

      switch (beslutsutfall)
      {
         case JA:
            return BeslutsutfallEnum.JA;
         case NEJ:
            return BeslutsutfallEnum.NEJ;
         case FU:
         default:
            return BeslutsutfallEnum.FU;
      }
   }
}
