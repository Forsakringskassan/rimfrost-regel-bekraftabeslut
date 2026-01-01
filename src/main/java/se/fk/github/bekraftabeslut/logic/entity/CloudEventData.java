package se.fk.github.bekraftabeslut.logic.entity;

import java.util.UUID;

import org.immutables.value.Value;

@Value.Immutable
public interface CloudEventData
{

   UUID id();

   UUID kogitorootprociid();

   UUID kogitoparentprociid();

   UUID kogitoprocinstanceid();

   String kogitorootprocid();

   String kogitoprocid();

   String kogitoprocist();

   String kogitoprocversion();

}
