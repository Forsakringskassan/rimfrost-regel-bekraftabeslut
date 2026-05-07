package se.fk.github.bekraftabeslut.storage;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;
import se.fk.rimfrost.framework.regel.manuell.storage.CloudEventDataStorage;
import se.fk.rimfrost.framework.regel.manuell.storage.ManuellRegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.ManuellRegelCommonData;

import java.util.HashMap;
import java.util.UUID;

@ApplicationScoped
public class CloudEventDataStorageService implements CloudEventDataStorage
{
   // Note: For most rules this should be some type of storage with persistence
   //       that allows for multiple instances of the rule to access the data.
   //       Do NOT use the implementation in this class in any production
   //       environment.

   HashMap<UUID, CloudEventData> map = new HashMap<>();

   @Override
   public CloudEventData getCloudEventData(UUID handlaggningId)
   {
      return map.get(handlaggningId);
   }

   @Override
   public void setCloudEventData(UUID handlaggningId, CloudEventData cloudEventData)
   {
      map.put(handlaggningId, cloudEventData);
   }

   @Override
   public void deleteCloudEventData(UUID handlaggningId)
   {
      map.remove(handlaggningId);
   }
}
