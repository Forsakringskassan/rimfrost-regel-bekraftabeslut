package se.fk.github.bekraftabeslut.logic;

import java.time.ZoneOffset;
import java.util.ArrayList;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.bekraftabeslut.integration.arbetsgivare.dto.ArbetsgivareResponse;
import se.fk.github.bekraftabeslut.integration.folkbokford.dto.FolkbokfordResponse;
import se.fk.github.bekraftabeslut.integration.kafka.dto.ImmutableBekraftaBeslutResponseRequest;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.*;
import se.fk.github.bekraftabeslut.logic.dto.ImmutableErsattning;
import se.fk.github.bekraftabeslut.logic.dto.ImmutableGetBekraftaBeslutDataResponse;
import se.fk.github.bekraftabeslut.logic.dto.Beslutsutfall;
import se.fk.github.bekraftabeslut.logic.dto.GetBekraftaBeslutDataResponse;
import se.fk.github.bekraftabeslut.logic.dto.GetBekraftaBeslutDataResponse.Ersattning;
import se.fk.github.bekraftabeslut.logic.entity.CloudEventData;
import se.fk.github.bekraftabeslut.logic.entity.ErsattningData;
import se.fk.github.bekraftabeslut.logic.entity.BekraftaBeslutData;
import se.fk.rimfrost.framework.integration.kafka.dto.ImmutableRegelResponse;
import se.fk.rimfrost.framework.integration.kafka.dto.RegelResponse;
import se.fk.rimfrost.framework.logic.config.RegelConfig;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Ersattning.BeslutsutfallEnum;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Roll;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Verksamhetslogik;
import se.fk.rimfrost.regel.common.Utfall;


@ApplicationScoped
public class BekraftaBeslutMapper
{

   public GetBekraftaBeslutDataResponse toBekraftaBeslutResponse(KundbehovsflodeResponse kundbehovflodesResponse,
         FolkbokfordResponse folkbokfordResponse, ArbetsgivareResponse arbetsgivareResponse,
         BekraftaBeslutData bekraftaBeslutData)
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

   public RegelResponse toRegelResponse(BekraftaBeslutData bekraftaBeslutData,
                                        CloudEventData cloudevent, Utfall utfall)
   {
      return ImmutableRegelResponse.builder()
            .id(cloudevent.id())
            .kundbehovsflodeId(bekraftaBeslutData.kundbehovsflodeId())
            .kogitoparentprociid(cloudevent.kogitoparentprociid())
            .kogitorootprociid(cloudevent.kogitorootprociid())
            .kogitoprocid(cloudevent.kogitoprocid())
            .kogitorootprocid(cloudevent.kogitorootprocid())
            .kogitoprocinstanceid(cloudevent.kogitoprocinstanceid())
            .kogitoprocist(cloudevent.kogitoprocist())
            .kogitoprocversion(cloudevent.kogitoprocversion())
            .utfall(utfall)
            .build();
   }

    public UpdateKundbehovsflodeRequest toUpdateKundbehovsflodeRequest(BekraftaBeslutData bekraftaBeslutData, RegelConfig regelConfig)
    {

        var lagrum = ImmutableUpdateKundbehovsflodeLagrum.builder()
                .id(regelConfig.getLagrum().getId())
                .version(regelConfig.getLagrum().getVersion())
                .forfattning(regelConfig.getLagrum().getForfattning())
                .giltigFrom(regelConfig.getLagrum().getGiltigFom().toInstant().atOffset(ZoneOffset.UTC))
                .kapitel(regelConfig.getLagrum().getKapitel())
                .paragraf(regelConfig.getLagrum().getParagraf())
                .stycke(regelConfig.getLagrum().getStycke())
                .punkt(regelConfig.getLagrum().getPunkt())
                .build();

        var regel = ImmutableUpdateKundbehovsflodeRegel.builder()
                .id(regelConfig.getRegel().getId())
                .beskrivning(regelConfig.getRegel().getBeskrivning())
                .namn(regelConfig.getRegel().getNamn())
                .version(regelConfig.getRegel().getVersion())
                .lagrum(lagrum)
                .build();

        var specifikation = ImmutableUpdateKundbehovsflodeSpecifikation.builder()
                .id(regelConfig.getSpecifikation().getId())
                .version(regelConfig.getSpecifikation().getVersion())
                .namn(regelConfig.getSpecifikation().getNamn())
                .uppgiftsbeskrivning(regelConfig.getSpecifikation().getUppgiftbeskrivning())
                .verksamhetslogik(Verksamhetslogik.fromString(regelConfig.getSpecifikation().getVerksamhetslogik()))
                .roll(Roll.fromString(regelConfig.getSpecifikation().getRoll()))
                .applikationsId(regelConfig.getSpecifikation().getApplikationsId())
                .applikationsversion(regelConfig.getSpecifikation().getApplikationsversion())
                .url(regelConfig.getUppgift().getPath())
                .regel(regel)
                .build();

        var uppgift = ImmutableUpdateKundbehovsflodeUppgift.builder()
                .id(bekraftaBeslutData.uppgiftId())
                .version(regelConfig.getUppgift().getVersion())
                .skapadTs(bekraftaBeslutData.skapadTs())
                .utfordTs(bekraftaBeslutData.utfordTs())
                .planeradTs(bekraftaBeslutData.planeradTs())
                .utforarId(bekraftaBeslutData.utforarId())
                .uppgiftStatus(bekraftaBeslutData.uppgiftStatus())
                .aktivitet(regelConfig.getUppgift().getAktivitet())
                .fsSAinformation(bekraftaBeslutData.fssaInformation())
                .specifikation(specifikation)
                .build();

        var requestBuilder = ImmutableUpdateKundbehovsflodeRequest.builder()
                .kundbehovsflodeId(bekraftaBeslutData.kundbehovsflodeId())
                .uppgift(uppgift)
                .underlag(new ArrayList<UpdateKundbehovsflodeUnderlag>());

        for (ErsattningData rtfErsattning : bekraftaBeslutData.ersattningar())
        {
            var ersattning = ImmutableUpdateKundbehovsflodeErsattning.builder()
                    .beslutsutfall(mapBeslutsutfall(rtfErsattning.beslutsutfall()))
                    .id(rtfErsattning.id())
                    .avslagsanledning(rtfErsattning.avslagsanledning())
                    .build();
            requestBuilder.addErsattningar(ersattning);
        }

        for (var rtfUnderlag : bekraftaBeslutData.underlag())
        {
            var underlag = ImmutableUpdateKundbehovsflodeUnderlag.builder()
                    .typ(rtfUnderlag.typ())
                    .version(rtfUnderlag.version())
                    .data(rtfUnderlag.data())
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
