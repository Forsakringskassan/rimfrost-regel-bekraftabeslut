package se.fk.github.bekraftabeslut.logic.dto;

import java.util.UUID;

import org.immutables.value.Value;

@Value.Immutable
public interface CreateBekraftaBeslutDataRequest
{
   UUID id();

   UUID kundbehovsflodeId();

   String kogitorootprocid();

   UUID kogitorootprociid();

   UUID kogitoparentprociid();

   String kogitoprocid();

   UUID kogitoprocinstanceid();

   String kogitoprocist();

   String kogitoprocversion();

}
