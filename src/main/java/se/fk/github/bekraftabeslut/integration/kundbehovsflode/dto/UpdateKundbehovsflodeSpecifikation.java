package se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto;

import org.immutables.value.Value;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Roll;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Verksamhetslogik;

import java.util.UUID;

@Value.Immutable
public interface UpdateKundbehovsflodeSpecifikation
{

   UUID id();

   String version();

   String namn();

   String uppgiftsbeskrivning();

   Verksamhetslogik verksamhetslogik();

   Roll roll();

   String applikationsId();

   String applikationsversion();

   String url();

   UpdateKundbehovsflodeRegel regel();
}
