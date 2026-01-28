package se.fk.github.bekraftabeslut.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import se.fk.github.bekraftabeslut.integration.arbetsgivare.ArbetsgivareAdapter;
import se.fk.github.bekraftabeslut.integration.arbetsgivare.dto.ArbetsgivareResponse;
import se.fk.github.bekraftabeslut.integration.arbetsgivare.dto.ImmutableArbetsgivareRequest;
import se.fk.github.bekraftabeslut.integration.folkbokford.FolkbokfordAdapter;
import se.fk.github.bekraftabeslut.integration.folkbokford.dto.FolkbokfordResponse;
import se.fk.github.bekraftabeslut.integration.folkbokford.dto.ImmutableFolkbokfordRequest;
import se.fk.github.bekraftabeslut.integration.kafka.BekraftaBeslutKafkaProducer;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.KundbehovsflodeAdapter;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.ImmutableKundbehovsflodeRequest;
import se.fk.github.bekraftabeslut.logic.entity.CloudEventData;
import se.fk.github.bekraftabeslut.logic.entity.ImmutableCloudEventData;
import se.fk.github.bekraftabeslut.logic.entity.ImmutableErsattningData;
import se.fk.github.bekraftabeslut.logic.entity.ImmutableBekraftaBeslutData;
import se.fk.github.bekraftabeslut.logic.entity.ImmutableUnderlag;
import se.fk.github.bekraftabeslut.logic.entity.BekraftaBeslutData;
import se.fk.rimfrost.Status;
import se.fk.github.bekraftabeslut.logic.entity.ErsattningData;
import se.fk.github.bekraftabeslut.logic.dto.Beslutsutfall;
import se.fk.github.bekraftabeslut.logic.dto.CreateBekraftaBeslutDataRequest;
import se.fk.github.bekraftabeslut.logic.dto.GetBekraftaBeslutDataRequest;
import se.fk.github.bekraftabeslut.logic.dto.GetBekraftaBeslutDataResponse;
import se.fk.github.bekraftabeslut.logic.dto.UpdateErsattningDataRequest;
import se.fk.github.bekraftabeslut.logic.dto.UpdateBekraftaBeslutDataRequest;
import se.fk.github.bekraftabeslut.logic.dto.UpdateStatusRequest;
import se.fk.rimfrost.framework.integration.config.RegelConfigProvider;
import se.fk.rimfrost.framework.integration.config.RegelConfigProviderYaml;
import se.fk.rimfrost.framework.integration.kafka.OulKafkaProducer;
import se.fk.rimfrost.framework.integration.kafka.RegelKafkaProducer;
import se.fk.rimfrost.framework.integration.kafka.dto.ImmutableOulMessageRequest;
import se.fk.rimfrost.framework.logic.config.RegelConfig;
import se.fk.rimfrost.framework.logic.dto.OulResponse;
import se.fk.rimfrost.framework.logic.dto.OulStatus;
import se.fk.rimfrost.framework.logic.dto.RegelDataRequest;
import se.fk.rimfrost.framework.presentation.kafka.OulHandlerInterface;
import se.fk.rimfrost.framework.presentation.kafka.RegelRequestHandlerInterface;
import se.fk.rimfrost.regel.common.Utfall;


@ApplicationScoped
@Startup
public class BekraftaBeslutService implements RegelRequestHandlerInterface, OulHandlerInterface
{

   @Inject
   RegelConfigProviderYaml regelConfigProvider;

   @Inject
   ObjectMapper objectMapper;

   @Inject
   BekraftaBeslutKafkaProducer kafkaProducer;

    @Inject
    RegelKafkaProducer regelKafkaProducer;

    OulKafkaProducer oulKafkaProducer;

   @Inject
   BekraftaBeslutMapper mapper;
   @Inject
   FolkbokfordAdapter folkbokfordAdapter;

   @Inject
   ArbetsgivareAdapter arbetsgivareAdapter;

   @Inject
   KundbehovsflodeAdapter kundbehovsflodeAdapter;

   private RegelConfig regelConfig;

   Map<UUID, CloudEventData> cloudevents = new HashMap<UUID, CloudEventData>();
   Map<UUID, BekraftaBeslutData> bekraftaBeslutDatas = new HashMap<UUID, BekraftaBeslutData>();

   @SuppressWarnings("unused")
   @PostConstruct
   void init()
   {
      this.regelConfig = regelConfigProvider.getConfig();
   }

   public GetBekraftaBeslutDataResponse getData(GetBekraftaBeslutDataRequest request) throws JsonProcessingException
   {
      var kundbehovsflodeRequest = ImmutableKundbehovsflodeRequest.builder()
            .kundbehovsflodeId(request.kundbehovsflodeId())
            .build();
      var kundbehovflodesResponse = kundbehovsflodeAdapter.getKundbehovsflodeInfo(kundbehovsflodeRequest);
      var folkbokfordRequest = ImmutableFolkbokfordRequest.builder()
            .personnummer(kundbehovflodesResponse.personnummer())
            .build();
      var folkbokfordResponse = folkbokfordAdapter.getFolkbokfordInfo(folkbokfordRequest);
      var arbetsgivareRequest = ImmutableArbetsgivareRequest.builder()
            .personnummer(kundbehovflodesResponse.personnummer())
            .build();
      var arbetsgivareResponse = arbetsgivareAdapter.getArbetsgivareInfo(arbetsgivareRequest);

      var bekraftaBeslutData = bekraftaBeslutDatas.get(request.kundbehovsflodeId());

      updateBekraftaBeslutDataUnderlag(bekraftaBeslutData, folkbokfordResponse, arbetsgivareResponse);

      updateKundbehovsflodeInfo(bekraftaBeslutData);

      return mapper.toBekraftaBeslutResponse(kundbehovflodesResponse, folkbokfordResponse, arbetsgivareResponse, bekraftaBeslutData);
   }

   @Override
   public void handleRegelRequest(RegelDataRequest request)
   {
      var kundbehovsflodeRequest = ImmutableKundbehovsflodeRequest.builder()
            .kundbehovsflodeId(request.kundbehovsflodeId())
            .build();
      var kundbehovflodesResponse = kundbehovsflodeAdapter.getKundbehovsflodeInfo(kundbehovsflodeRequest);

      var cloudeventData = ImmutableCloudEventData.builder()
            .id(request.id())
            .kogitoparentprociid(request.kogitoparentprociid())
            .kogitoprocid(request.kogitoprocid())
            .kogitoprocinstanceid(request.kogitoprocinstanceid())
            .kogitoprocist(request.kogitoprocist())
            .kogitoprocversion(request.kogitoprocversion())
            .kogitorootprocid(request.kogitorootprocid())
            .kogitorootprociid(request.kogitorootprociid())
            .build();

      var ersattninglist = new ArrayList<ErsattningData>();

      for (var ersattning : kundbehovflodesResponse.ersattning())
      {
         var ersattningData = ImmutableErsattningData.builder()
               .id(ersattning.ersattningsId())
               .build();
         ersattninglist.add(ersattningData);
      }

      var bekraftaBeslutData = ImmutableBekraftaBeslutData.builder()
            .kundbehovsflodeId(request.kundbehovsflodeId())
            .uppgiftId(UUID.randomUUID())
            .cloudeventId(cloudeventData.id())
            .ersattningar(ersattninglist)
            .underlag(new ArrayList<>())
            .build();

      cloudevents.put(cloudeventData.id(), cloudeventData);
      bekraftaBeslutDatas.put(bekraftaBeslutData.kundbehovsflodeId(), bekraftaBeslutData);
      var oulMessageRequest = ImmutableOulMessageRequest.builder()
            .kundbehovsflodeId(request.kundbehovsflodeId())
            .kundbehov("Vård av husdjur")
            .regel(regelConfig.getSpecifikation().getNamn())
            .beskrivning(regelConfig.getSpecifikation().getUppgiftbeskrivning())
            .verksamhetslogik(regelConfig.getSpecifikation().getVerksamhetslogik())
            .roll(regelConfig.getSpecifikation().getRoll())
            .url("http://localhost:8888" + regelConfig.getUppgift().getPath() + "/" + request.kundbehovsflodeId().toString())
            .build();
      oulKafkaProducer.sendOulRequest(oulMessageRequest);
   }

    @Override
   public void handleOulResponse(OulResponse oulResponse)
   {
      var bekraftaBeslutData = bekraftaBeslutDatas.get(oulResponse.kundbehovsflodeId());
      var updatedBekraftaBeslutData = ImmutableBekraftaBeslutData.builder()
            .from(bekraftaBeslutData)
            .uppgiftId(oulResponse.uppgiftId())
            .build();
      bekraftaBeslutDatas.put(updatedBekraftaBeslutData.kundbehovsflodeId(), updatedBekraftaBeslutData);
      updateKundbehovsflodeInfo(updatedBekraftaBeslutData);
   }

   public void updateErsattningData(UpdateErsattningDataRequest updateRequest)
   {
      var bekraftaBeslutData = bekraftaBeslutDatas.get(updateRequest.kundbehovsflodeId());

      var existingErsattning = bekraftaBeslutData.ersattningar().stream()
            .filter(e -> e.id().equals(updateRequest.ersattningId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("ErsattningData not found"));

      var updatedErsattning = ImmutableErsattningData.builder()
            .from(existingErsattning)
            .beslutsutfall(updateRequest.beslutsutfall())
            .avslagsanledning(updateRequest.avslagsanledning())
            .build();

      var updatedList = bekraftaBeslutData.ersattningar().stream()
            .map(e -> e.id().equals(updateRequest.ersattningId()) ? updatedErsattning : e)
            .toList();

      var updatedBekraftaBeslutData = ImmutableBekraftaBeslutData.builder()
            .from(bekraftaBeslutData)
            .ersattningar(updatedList)
            .build();

      bekraftaBeslutDatas.put(updateRequest.kundbehovsflodeId(), updatedBekraftaBeslutData);

      updateKundbehovsflodeInfo(updatedBekraftaBeslutData);

      if (updateRequest.signerad())
      {
         var allErsattningarApproved = updatedList.stream().allMatch(e -> e.beslutsutfall() == Beslutsutfall.JA);
         Utfall utfall;
         if (allErsattningarApproved) {
             utfall = Utfall.JA;
         } else {
             utfall = Utfall.NEJ;
         }
         var cloudevent = cloudevents.get(updatedBekraftaBeslutData.cloudeventId());
         var regelResponse = mapper.toRegelResponse(updatedBekraftaBeslutData, cloudevent, utfall);
         oulKafkaProducer.sendOulStatusUpdate(updatedBekraftaBeslutData.uppgiftId(), Status.AVSLUTAD);
         regelKafkaProducer.sendRegelResponse(regelResponse, "SOURCE_TODO");
      }
   }

   @Override
   public void handleOulStatus(OulStatus oulStatus)
   {
      BekraftaBeslutData bekraftaBeslutData = bekraftaBeslutDatas.values()
            .stream()
            .filter(r -> r.uppgiftId().equals(oulStatus.uppgiftId()))
            .findFirst()
            .orElse(bekraftaBeslutDatas.get(oulStatus.kundbehovsflodeId()));
      updateKundbehovsflodeInfo(bekraftaBeslutData);
   }

   private void updateBekraftaBeslutDataUnderlag(BekraftaBeslutData bekraftaBeslutData, FolkbokfordResponse folkbokfordResponse,
         ArbetsgivareResponse arbetsgivareResponse) throws JsonProcessingException
   {
      var bekraftaBeslutDataBuilder = ImmutableBekraftaBeslutData.builder().from(bekraftaBeslutData);

      if (folkbokfordResponse != null)
      {
         var folkbokfordUnderlag = ImmutableUnderlag.builder()
               .typ("FolkbokfördUnderlag")
               .version("1.0")
               .data(objectMapper.writeValueAsString(folkbokfordResponse))
               .build();
         bekraftaBeslutDataBuilder.addUnderlag(folkbokfordUnderlag);
      }

      if (arbetsgivareResponse != null)
      {
         var arbetsgivareUnderlag = ImmutableUnderlag.builder()
               .typ("ArbetsgivareUnderlag")
               .version("1.0")
               .data(objectMapper.writeValueAsString(arbetsgivareResponse))
               .build();
         bekraftaBeslutDataBuilder.addUnderlag(arbetsgivareUnderlag);
      }

      bekraftaBeslutDatas.put(bekraftaBeslutData.kundbehovsflodeId(), bekraftaBeslutDataBuilder.build());
   }

   private void updateKundbehovsflodeInfo(BekraftaBeslutData bekraftaBeslutData)
   {
      var request = mapper.toUpdateKundbehovsflodeRequest(bekraftaBeslutData, regelConfigProvider.getConfig());
      kundbehovsflodeAdapter.updateKundbehovsflodeInfo(request);
   }
}
