package se.fk.github.bekraftabeslut.logic.entity;

import org.immutables.value.Value;

@Value.Immutable
public interface Beslutsdata
{
   String avslutstyp();

   String beslutstyp();

   String beslutsutfall();
}
