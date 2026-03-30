package se.fk.github.bekraftabeslut.storage;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.regel.manuell.storage.ManuellRegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.ManuellRegelCommonData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class BekraftaBeslutDataStorage implements ManuellRegelCommonDataStorage
{
   // Note: For most rules this should be some type of storage with persistence
   //       that allows for multiple instances of the rule to access the data.
   //       Do NOT use the implementation in this class in any production
   //       environment.

   private final Map<UUID, ManuellRegelCommonData> regelDatas = new HashMap<>();
   private final Object lock = new Object();

   @Override
   public ManuellRegelCommonData getManuellRegelCommonData(UUID handlaggningId)
   {
      synchronized (lock)
      {
         return regelDatas.get(handlaggningId);
      }
   }

   @Override
   public void setManuellRegelCommonData(UUID handlaggningId, ManuellRegelCommonData manuellRegelCommonData)
   {
      synchronized (lock)
      {
         regelDatas.put(handlaggningId, manuellRegelCommonData);
      }
   }

   @Override
   public void deleteManuellRegelCommonData(UUID handlaggningId)
   {
      synchronized (lock)
      {
         regelDatas.remove(handlaggningId);
      }
   }
}
