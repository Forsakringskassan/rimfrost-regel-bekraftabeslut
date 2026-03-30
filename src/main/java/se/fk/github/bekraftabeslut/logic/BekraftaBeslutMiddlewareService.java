package se.fk.github.bekraftabeslut.logic;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellMiddlewareService;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.PatchDataRequest;

@ApplicationScoped
public class BekraftaBeslutMiddlewareService extends RegelManuellMiddlewareService<GetDataResponse, PatchDataRequest>
{
}
