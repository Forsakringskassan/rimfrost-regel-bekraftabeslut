package se.fk.github.bekraftabeslut.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import se.fk.github.bekraftabeslut.integration.kafka.BekraftaBeslutKafkaProducer;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.KundbehovsflodeAdapter;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.ImmutableKundbehovsflodeRequest;
import se.fk.github.bekraftabeslut.logic.dto.CreateBekraftaBeslutDataRequest;
import se.fk.github.bekraftabeslut.logic.entity.ErsattningData;
import se.fk.github.bekraftabeslut.logic.entity.ImmutableBekraftaBeslutData;
import se.fk.github.bekraftabeslut.logic.entity.BekraftaBeslutData;
import se.fk.github.bekraftabeslut.logic.entity.ImmutableCloudEventData;
import se.fk.github.bekraftabeslut.logic.entity.CloudEventData;
import se.fk.github.bekraftabeslut.logic.entity.ImmutableErsattningData;

@ApplicationScoped
public class BekraftaBeslutService {

    @Inject 
    KundbehovsflodeAdapter kundbehovsflodeAdapter;

    @Inject
    BekraftaBeslutKafkaProducer kafkaProducer;

    Map<UUID, CloudEventData> cloudevents = new HashMap<>();
    Map<UUID, BekraftaBeslutData> bekraftaBeslutDatas = new HashMap<>();
    
    
    public void createBekraftaBeslutData(CreateBekraftaBeslutDataRequest request) {  
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
                .cloudeventId(cloudeventData.id())
                .ersattningar(ersattninglist)
                .build();

        cloudevents.put(cloudeventData.id(), cloudeventData);
        bekraftaBeslutDatas.put(bekraftaBeslutData.kundbehovsflodeId(), bekraftaBeslutData);

        kafkaProducer.sendOulRequest(request.kundbehovsflodeId());
    }
}
