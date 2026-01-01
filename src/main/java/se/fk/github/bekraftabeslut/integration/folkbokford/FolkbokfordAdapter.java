package se.fk.github.bekraftabeslut.integration.folkbokford;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import se.fk.github.jaxrsclientfactory.JaxrsClientFactory;
import se.fk.github.jaxrsclientfactory.JaxrsClientOptionsBuilders;
import se.fk.github.bekraftabeslut.integration.folkbokford.dto.FolkbokfordRequest;
import se.fk.github.bekraftabeslut.integration.folkbokford.dto.FolkbokfordResponse;
import se.fk.rimfrost.api.folkbokforing.jaxrsspec.controllers.generatedsource.FolkbokforingControllerApi;

@ApplicationScoped
public class FolkbokfordAdapter
{

   @ConfigProperty(name = "folkbokford.api.base-url")
   String folkbokfordBaseUrl;

   @Inject
   FolkbokfordMapper mapper;

   private FolkbokforingControllerApi folkbokfordClient;

   @PostConstruct
   void init()
   {
      this.folkbokfordClient = new JaxrsClientFactory()
            .create(JaxrsClientOptionsBuilders.createClient(folkbokfordBaseUrl, FolkbokforingControllerApi.class)
                  .build());
   }

   public FolkbokfordResponse getFolkbokfordInfo(FolkbokfordRequest folkbokfordRequest)
   {
      var apiResponse = folkbokfordClient.folkbokforingPersnrGet(folkbokfordRequest.personnummer());
      return mapper.toFolkbokfordResponse(apiResponse);
   }
}
