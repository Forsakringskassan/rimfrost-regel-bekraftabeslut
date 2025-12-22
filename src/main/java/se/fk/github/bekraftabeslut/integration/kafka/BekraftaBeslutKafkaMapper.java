package se.fk.github.bekraftabeslut.integration.kafka;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.bekraftabeslut.integration.kafka.dto.BekraftaBeslutResponseRequest;
import se.fk.rimfrost.regel.bekraftabeslut.KogitoProcType;
import se.fk.rimfrost.regel.bekraftabeslut.RattTillForsakring;
import se.fk.rimfrost.regel.bekraftabeslut.BekraftaBeslutResponseMessageData;
import se.fk.rimfrost.regel.bekraftabeslut.BekraftaBeslutResponseMessagePayload;
import se.fk.rimfrost.regel.bekraftabeslut.SpecVersion;

@ApplicationScoped
public class BekraftaBeslutKafkaMapper
{
   public BekraftaBeslutResponseMessagePayload toBekraftaBeslutResponse(BekraftaBeslutResponseRequest request)
   {
      var data = new BekraftaBeslutResponseMessageData();
       data.setKundbehovsflodeId(request.kundbehovsflodeId().toString());
       data.setRattTillForsakring(request.rattTillForsakring() ? RattTillForsakring.JA : RattTillForsakring.NEJ); //DISKUTERA MED ULF

      var response = new BekraftaBeslutResponseMessagePayload();
      response.setId(request.id().toString());
      response.setKogitorootprocid(request.kogitorootprocid());
      response.setKogitorootprociid(request.kogitorootprociid().toString());
      response.setKogitoparentprociid(request.kogitoparentprociid().toString());
      response.setKogitoprocid(request.kogitoprocid());
      response.setKogitoprocinstanceid(request.kogitoprocinstanceid().toString());
      response.setKogitoprocrefid(request.kogitoprocinstanceid().toString());
      response.setKogitoprocist(request.kogitoprocist());
      response.setKogitoprocversion(request.kogitoprocversion());
      response.setSpecversion(SpecVersion.NUMBER_1_DOT_0);
      response.setSource("/regel/bekrafta-beslut");
      response.setType("bekrafta-beslut-responses");
      response.setKogitoproctype(KogitoProcType.BPMN);
      response.setData(data);

      return response;
   }
}
