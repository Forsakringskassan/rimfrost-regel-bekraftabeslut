package se.fk.github.bekraftabeslut.logic.entity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.immutables.value.Value;
import jakarta.annotation.Nullable;
import se.fk.github.bekraftabeslut.logic.dto.UppgiftStatus;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.FSSAinformation;

@Value.Immutable
public interface BekraftaBeslutData
{

   UUID kundbehovsflodeId();

   UUID cloudeventId();

   @Nullable
   UUID uppgiftId();

   @Nullable
   UUID utforarId();

   OffsetDateTime skapadTs();

   @Nullable
   OffsetDateTime utfordTs();

   @Nullable
   OffsetDateTime planeradTs();

   UppgiftStatus uppgiftStatus();

   FSSAinformation fssaInformation();

   List<ErsattningData> ersattningar();

   List<Underlag> underlag();

}
