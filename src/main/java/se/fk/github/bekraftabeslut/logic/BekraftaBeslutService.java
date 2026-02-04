package se.fk.github.bekraftabeslut.logic;

import java.time.OffsetDateTime;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import se.fk.github.bekraftabeslut.integration.arbetsgivare.ArbetsgivareAdapter;
import se.fk.github.bekraftabeslut.integration.arbetsgivare.dto.ArbetsgivareResponse;
import se.fk.github.bekraftabeslut.integration.arbetsgivare.dto.ImmutableArbetsgivareRequest;
import se.fk.github.bekraftabeslut.integration.folkbokford.FolkbokfordAdapter;
import se.fk.github.bekraftabeslut.integration.folkbokford.dto.FolkbokfordResponse;
import se.fk.github.bekraftabeslut.integration.folkbokford.dto.ImmutableFolkbokfordRequest;
import se.fk.github.bekraftabeslut.logic.dto.GetBekraftaBeslutDataRequest;
import se.fk.github.bekraftabeslut.logic.dto.GetBekraftaBeslutDataResponse;
import se.fk.github.bekraftabeslut.logic.dto.UpdateErsattningDataRequest;
import se.fk.github.bekraftabeslut.logic.entity.ImmutableErsattningData;
import se.fk.github.bekraftabeslut.logic.entity.ImmutableBekraftaBeslutData;
import se.fk.github.bekraftabeslut.logic.entity.ImmutableUnderlag;
import se.fk.github.bekraftabeslut.logic.entity.BekraftaBeslutData;
import se.fk.rimfrost.Status;
import se.fk.github.bekraftabeslut.logic.entity.ErsattningData;
import se.fk.rimfrost.framework.oul.integration.kafka.OulKafkaProducer;
import se.fk.rimfrost.framework.oul.integration.kafka.dto.ImmutableOulMessageRequest;
import se.fk.rimfrost.framework.oul.logic.dto.OulResponse;
import se.fk.rimfrost.framework.oul.logic.dto.OulStatus;
import se.fk.rimfrost.framework.oul.presentation.kafka.OulHandlerInterface;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.integration.config.RegelConfigProviderYaml;
import se.fk.rimfrost.framework.regel.integration.kafka.RegelKafkaProducer;
import se.fk.rimfrost.framework.regel.integration.kundbehovsflode.KundbehovsflodeAdapter;
import se.fk.rimfrost.framework.regel.integration.kundbehovsflode.dto.ImmutableKundbehovsflodeRequest;
import se.fk.rimfrost.framework.regel.logic.RegelMapper;
import se.fk.rimfrost.framework.regel.logic.config.RegelConfig;
import se.fk.rimfrost.framework.regel.logic.dto.Beslutsutfall;
import se.fk.rimfrost.framework.regel.logic.dto.RegelDataRequest;
import se.fk.rimfrost.framework.regel.logic.dto.UppgiftStatus;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;
import se.fk.rimfrost.framework.regel.logic.entity.ImmutableCloudEventData;
import se.fk.rimfrost.framework.regel.presentation.kafka.RegelRequestHandlerInterface;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.FSSAinformation;

@ApplicationScoped
@Startup
public class BekraftaBeslutService implements RegelRequestHandlerInterface, OulHandlerInterface
{

   @ConfigProperty(name = "mp.messaging.outgoing.regel-responses.topic")
   String responseTopic;

   @ConfigProperty(name = "kafka.source")
   String kafkaSource;

   @Inject
   RegelConfigProviderYaml regelConfigProvider;

   @Inject
   ObjectMapper objectMapper;

   @Inject
   RegelKafkaProducer regelKafkaProducer;

   @Inject
   OulKafkaProducer oulKafkaProducer;

   @Inject
   BekraftaBeslutMapper bekraftaBeslutMapper;

   @Inject
   RegelMapper regelMapper;

   @Inject
   FolkbokfordAdapter folkbokfordAdapter;

   @Inject
   ArbetsgivareAdapter arbetsgivareAdapter;

   @Inject
   KundbehovsflodeAdapter kundbehovsflodeAdapter;

   private RegelConfig regelConfig;

   Map<UUID, CloudEventData> cloudevents = new HashMap<>();
   Map<UUID, BekraftaBeslutData> bekraftaBeslutDatas = new HashMap<>();

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

      return bekraftaBeslutMapper.toBekraftaBeslutResponse(kundbehovflodesResponse, folkbokfordResponse, arbetsgivareResponse,
            bekraftaBeslutData);
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
            .type(responseTopic)
            .source(kafkaSource)
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
            .skapadTs(OffsetDateTime.now())
            .uppgiftStatus(UppgiftStatus.PLANERAD)
            .fssaInformation(FSSAinformation.HANDLAGGNING_PAGAR)
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
      var request = bekraftaBeslutMapper.toUpdateKundbehovsflodeRequest(bekraftaBeslutData, regelConfigProvider.getConfig());
      kundbehovsflodeAdapter.updateKundbehovsflodeInfo(request);

   }

   public void setUppgiftDone(UUID kundbehovsflodeId)
   {
      var bekraftaBeslutData = bekraftaBeslutDatas.get(kundbehovsflodeId);

      var updatedBekraftaBeslutDataBuilder = ImmutableBekraftaBeslutData.builder()
            .from(bekraftaBeslutData);

      updatedBekraftaBeslutDataBuilder.uppgiftStatus(UppgiftStatus.AVSLUTAD);

      var updatedBekraftaBeslutData = updatedBekraftaBeslutDataBuilder.build();
      bekraftaBeslutDatas.put(kundbehovsflodeId, updatedBekraftaBeslutData);

      var utfall = bekraftaBeslutData.ersattningar().stream().allMatch(e -> e.beslutsutfall() == Beslutsutfall.JA) ? Utfall.JA
            : Utfall.NEJ;
      var cloudevent = cloudevents.get(updatedBekraftaBeslutData.cloudeventId());
      var regelResponse = regelMapper.toRegelResponse(kundbehovsflodeId, cloudevent, utfall);
      oulKafkaProducer.sendOulStatusUpdate(updatedBekraftaBeslutData.uppgiftId(), Status.AVSLUTAD);
      regelKafkaProducer.sendRegelResponse(regelResponse);

      updateKundbehovsflodeInfo(updatedBekraftaBeslutData);
   }
}
