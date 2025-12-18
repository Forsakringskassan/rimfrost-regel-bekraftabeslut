package se.fk.github.bekraftabeslut.logic.entity;

import java.util.List;
import java.util.UUID;

import org.immutables.value.Value;

import jakarta.annotation.Nullable;

@Value.Immutable
public interface BekraftaBeslutData
{

   UUID kundbehovsflodeId();

   UUID cloudeventId();

   @Nullable
   UUID uppgiftId();

   List<ErsattningData> ersattningar();

}
