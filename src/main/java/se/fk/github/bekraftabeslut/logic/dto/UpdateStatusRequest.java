package se.fk.github.bekraftabeslut.logic.dto;

import java.util.UUID;
import org.immutables.value.Value;
import se.fk.rimfrost.framework.regel.logic.dto.UppgiftStatus;

@Value.Immutable
public interface UpdateStatusRequest
{
   UUID handlaggningId();

   UUID uppgiftId();

   UppgiftStatus uppgiftStatus();

}
