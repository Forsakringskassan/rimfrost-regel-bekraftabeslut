package se.fk.github.bekraftabeslut.presentation.rest;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import se.fk.github.bekraftabeslut.logic.BekraftaBeslutService;
import se.fk.github.bekraftabeslut.logic.dto.ImmutableGetBekraftaBeslutDataRequest;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.RegelBekraftaBeslutControllerApi;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.PatchDataRequest;

@ApplicationScoped
@Path("/regel/bekrafta-beslut/{kundbehovsflodeId}")
public class BekraftaBeslutController implements RegelBekraftaBeslutControllerApi {

   private static final Logger LOGGER = LoggerFactory.getLogger(BekraftaBeslutController.class);

   @Inject
   BekraftaBeslutService bekraftaBeslutService;

   @Inject
   BekraftaBeslutRestMapper mapper;

   @GET
   @Override
   public GetDataResponse getData(UUID kundbehovsflodeId) {
      try {
         var request = ImmutableGetBekraftaBeslutDataRequest.builder()
               .kundbehovsflodeId(kundbehovsflodeId).build();
         var response = bekraftaBeslutService.getData(request);
         return mapper.toGetDataResponse(response);
      } catch (JsonProcessingException e) {
         throw new InternalServerErrorException("Failed to process request");
      }
   }

   @PATCH
   @Override
   public void updateData(UUID kundbehovsflodeId, @Valid @NotNull PatchDataRequest patchRequest) {
      LOGGER.info(
            "updateData received with patchrequest: " + patchRequest);
      var request = mapper.toUpdateErsattningDataRequest(kundbehovsflodeId, patchRequest);
      bekraftaBeslutService.updateErsattningData(request);
   }
}
