package se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto;

import java.util.UUID;

import org.immutables.value.Value;

import jakarta.annotation.Nullable;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Ersattning.BeslutsutfallEnum;

@Value.Immutable
public interface UpdateKundbehovsflodeErsattning
{

   UUID id();

   @Nullable
   BeslutsutfallEnum beslutsutfall();

   @Nullable
   String avslagsanledning();

}
