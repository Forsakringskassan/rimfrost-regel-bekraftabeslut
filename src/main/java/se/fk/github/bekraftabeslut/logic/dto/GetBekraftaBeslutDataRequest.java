package se.fk.github.bekraftabeslut.logic.dto;

import java.util.UUID;

import org.immutables.value.Value;

@Value.Immutable
public interface GetBekraftaBeslutDataRequest
{

   UUID kundbehovsflodeId();

}
