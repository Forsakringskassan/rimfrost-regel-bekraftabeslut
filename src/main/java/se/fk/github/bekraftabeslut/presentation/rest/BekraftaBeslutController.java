package se.fk.github.bekraftabeslut.presentation.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.bekraftabeslut.logic.BekraftaBeslutService;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.BekraftaBeslutControllerApi;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.PatchDataRequest;
import se.fk.rimfrost.framework.regel.manuell.presentation.rest.RegelManuellController;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Referensdata;

import java.util.List;

@ApplicationScoped
@Path("/regel/bekraftabeslut")
public class BekraftaBeslutController extends RegelManuellController<GetDataResponse, PatchDataRequest>
      implements BekraftaBeslutControllerApi
{
   @Inject
   BekraftaBeslutService bekraftaBeslutService;

   @Override
   @GET
   @Path("/avslutstyp")
   public List<Referensdata> getAvslutstyp()
   {
      return bekraftaBeslutService.getAvslutstyp();
   }

   @Override
   @GET
   @Path("/beslutstyp")
   public List<Referensdata> getBeslutstyp()
   {
      return bekraftaBeslutService.getBeslutstyp();
   }

   @Override
   @GET
   @Path("/beslutsutfallstyp")
   public List<Referensdata> getBeslutsutfallstyp()
   {
      return bekraftaBeslutService.getBeslutsutfallstyp();
   }

   @Override
   @GET
   @Path("/yrkandestatus")
   public List<Referensdata> getYrkandestatus()
   {
      return bekraftaBeslutService.getYrkandestatus();
   }
}
