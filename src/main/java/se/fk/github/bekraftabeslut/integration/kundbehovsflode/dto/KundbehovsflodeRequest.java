package se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto;

import java.util.UUID;

import org.immutables.value.Value;

@Value.Immutable
public interface KundbehovsflodeRequest
{

   UUID kundbehovsflodeId();

}
