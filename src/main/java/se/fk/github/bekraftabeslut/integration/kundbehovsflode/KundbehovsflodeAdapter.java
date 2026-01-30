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
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class KundbehovsflodeAdapter
{

   @ConfigProperty(name = "kundbehovsflode.api.base-url")
   String kundbehovsflodeBaseUrl;

   @Inject
   KundbehovsflodeMapper mapper;

   private KundbehovsflodeControllerApi kundbehovsClient2;

   @Inject
   @RestClient
   KundbehovsflodeClient kundbehovsClient;

   @PostConstruct
   void init()
   {
      this.kundbehovsClient2 = new JaxrsClientFactory()
            .create(JaxrsClientOptionsBuilders.createClient(kundbehovsflodeBaseUrl, KundbehovsflodeControllerApi.class)
                  .build());
      System.out.printf("XXX KundbehovsflodeAdapter init kundbehovsflodeBaseUrl = %s%n", kundbehovsflodeBaseUrl);
   }

   public KundbehovsflodeResponse getKundbehovsflodeInfo(KundbehovsflodeRequest kundbehovsflodeRequest)
   {
      System.out.printf("HIT getKundbehovsflodeInfo %n");
      System.out.printf("HIT getKundbehovsflodeInfo kundbehovsflodeRequest.kundbehovsflodeId() = %s%n",
            kundbehovsflodeRequest.kundbehovsflodeId());

      try
      {
         System.out.printf("HIT getKundbehovsflodeInfo : kundbehovsClient %s%n", kundbehovsClient);
         System.out.printf("HIT getKundbehovsflodeInfo : kundbehovsflodeRequest %s%n", kundbehovsflodeRequest);
         System.out.printf("HIT getKundbehovsflodeInfo : kundbehovsflodeRequest.kundbehovsflodeId() %s%n",
               kundbehovsflodeRequest.kundbehovsflodeId());

         var apiResponse = kundbehovsClient.getKundbehovsflode(kundbehovsflodeRequest.kundbehovsflodeId());

         System.out.printf("HIT getKundbehovsflodeInfo 2%n");

         return mapper.toKundbehovsflodeResponse(apiResponse);
      }
      catch (Exception e)
      {
         System.out.printf("HIT getKundbehovsflodeInfo exception%n");
         e.printStackTrace(); // <-- prints the underlying Jackson exception
         throw e;
      }
   }

   public void updateKundbehovsflodeInfo(UpdateKundbehovsflodeRequest request)
   {
      var apiResponse = kundbehovsClient.getKundbehovsflode(request.kundbehovsflodeId());

      var apiRequest = mapper.toApiRequest(request, apiResponse);
      kundbehovsClient.putKundbehovsflode(request.kundbehovsflodeId(), apiRequest);
   }
}
