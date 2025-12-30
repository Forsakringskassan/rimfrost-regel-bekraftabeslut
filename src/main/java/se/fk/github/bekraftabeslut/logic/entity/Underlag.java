package se.fk.github.bekraftabeslut.logic.entity;

import org.immutables.value.Value;

@Value.Immutable
public interface Underlag
{

   String typ();

   String version();

   String data();

}
