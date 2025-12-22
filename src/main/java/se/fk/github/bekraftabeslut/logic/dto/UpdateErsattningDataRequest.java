package se.fk.github.bekraftabeslut.logic.dto;

import java.util.UUID;

import org.immutables.value.Value;

@Value.Immutable
public interface UpdateErsattningDataRequest
{

   UUID kundbehovsflodeId();

   UUID ersattningId();

   Beslutsutfall beslutsutfall();

   String avslagsanledning();

   boolean signernad();

}
