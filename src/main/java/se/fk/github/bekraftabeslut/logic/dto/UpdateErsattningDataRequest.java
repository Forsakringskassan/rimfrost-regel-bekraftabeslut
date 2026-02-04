package se.fk.github.bekraftabeslut.logic.dto;

import java.util.UUID;
import org.immutables.value.Value;
import se.fk.rimfrost.framework.regel.logic.dto.Beslutsutfall;

@Value.Immutable
public interface UpdateErsattningDataRequest
{

   UUID kundbehovsflodeId();

   UUID ersattningId();

   Beslutsutfall beslutsutfall();

   String avslagsanledning();

   boolean signerad();

}
