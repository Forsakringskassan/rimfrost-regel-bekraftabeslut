package se.fk.github.bekraftabeslut.storage;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.bekraftabeslut.storage.entity.BekraftaBeslutDataStorage;
import se.fk.rimfrost.framework.storage.DataStorageProvider;

@ApplicationScoped
public class BekraftaBeslutDataStorageProvider implements DataStorageProvider<BekraftaBeslutDataStorage>
{
   private BekraftaBeslutDataStorage storage;

   @PostConstruct
   public void init()
   {
      storage = new BekraftaBeslutDataStorage();
   }

   @Override
   public BekraftaBeslutDataStorage getDataStorage()
   {
      return storage;
   }
}
