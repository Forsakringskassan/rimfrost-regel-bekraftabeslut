package se.fk.github.bekraftabeslut.integration.kundbehovsflode;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.*;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/kundbehovsflode")
@RegisterRestClient(configKey = "kundbehovsflode")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface KundbehovsflodeClient
{

   @GET
   @Path("/{kundbehovsflodeId}")
   @ClientHeaderParam(name = "BREADCRUMB-ID", value = "test-breadcrumb")
   @ClientHeaderParam(name = "PROCESSID", value = "test-process")
   GetKundbehovsflodeResponse getKundbehovsflode(
         @PathParam("kundbehovsflodeId") UUID kundbehovsflodeId);

   @POST
   @Path("/{kundbehovsflodeId}")
   @ClientHeaderParam(name = "BREADCRUMB-ID", value = "test-breadcrumb")
   @ClientHeaderParam(name = "PROCESSID", value = "test-process")
   PostKundbehovsflodeResponse postKundbehovsflode(
         PostKundbehovsflodeRequest request);

   @PUT
   @Path("/{kundbehovsflodeId}")
   @ClientHeaderParam(name = "BREADCRUMB-ID", value = "test-breadcrumb")
   @ClientHeaderParam(name = "PROCESSID", value = "test-process")
   PutKundbehovsflodeResponse putKundbehovsflode(
         @PathParam("kundbehovsflodeId") UUID kundbehovsflodeId,
         PutKundbehovsflodeRequest request);
}
