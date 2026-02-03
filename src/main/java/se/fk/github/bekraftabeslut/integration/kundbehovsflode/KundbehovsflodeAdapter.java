package se.fk.github.bekraftabeslut.integration.kundbehovsflode;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import se.fk.github.jaxrsclientfactory.JaxrsClientFactory;
import se.fk.github.jaxrsclientfactory.JaxrsClientOptionsBuilders;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.KundbehovsflodeRequest;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.KundbehovsflodeResponse;
import se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto.UpdateKundbehovsflodeRequest;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.KundbehovsflodeControllerApi;

@ApplicationScoped
public class KundbehovsflodeAdapter
{

   @ConfigProperty(name = "kundbehovsflode.api.base-url")
   String kundbehovsflodeBaseUrl;

   @Inject
   KundbehovsflodeMapper mapper;

   private KundbehovsflodeControllerApi kundbehovsflodeClient;

   @PostConstruct
   void init()
   {
      this.kundbehovsflodeClient = new JaxrsClientFactory()
            .create(JaxrsClientOptionsBuilders.createClient(kundbehovsflodeBaseUrl, KundbehovsflodeControllerApi.class)
                  .build());
   }

   public KundbehovsflodeResponse getKundbehovsflodeInfo(KundbehovsflodeRequest kundbehovsflodeRequest)
   {
      try
      {
         var apiResponse = kundbehovsflodeClient.getKundbehovsflode(kundbehovsflodeRequest.kundbehovsflodeId());
         return mapper.toKundbehovsflodeResponse(apiResponse);
      }
      catch (Exception e)
      {
         throw new RuntimeException("Failed getKundbehovsflodeInfo", e);
      }
   }

   public void updateKundbehovsflodeInfo(UpdateKundbehovsflodeRequest request)
   {
      var apiResponse = kundbehovsflodeClient.getKundbehovsflode(request.kundbehovsflodeId());
      var apiRequest = mapper.toApiRequest(request, apiResponse);
      kundbehovsflodeClient.putKundbehovsflode(request.kundbehovsflodeId(), apiRequest);
   }
}
