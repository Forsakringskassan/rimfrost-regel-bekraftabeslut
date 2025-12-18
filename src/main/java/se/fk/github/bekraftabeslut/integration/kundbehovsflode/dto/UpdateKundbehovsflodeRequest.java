package se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto;

import java.util.List;
import java.util.UUID;

import org.immutables.value.Value;

import jakarta.annotation.Nullable;

@Value.Immutable
public interface UpdateKundbehovsflodeRequest
{
   UUID kundbehovsflodeId();

   @Nullable
   UUID uppgiftId();

   List<UpdateKundbehovsflodeErsattning> ersattningar();

}
