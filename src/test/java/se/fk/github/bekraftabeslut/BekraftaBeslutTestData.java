package se.fk.github.bekraftabeslut;

import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Beslut;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.PatchDataRequest;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.UpdateErsattning;
import java.util.UUID;

public class BekraftaBeslutTestData
{
   // TODO consider refactoring to avoid copy/paste of test data in wiremock mappings

   public static String ERSATTNINGSTYP = "042bd313-d5ef-4886-97c5-e0a1c828baca";
   public static String ERSATTNINGSID = "bc6e5150-29be-4758-a2ee-e241f1d7ccf7";
   public static String PRODUCERADE_RESULTAT_ID = "bc6e5150-29be-4758-a2ee-e241f1d7ccf7";
   public static String KUND_FORNAMN = "Lisa";
   public static String KUND_EFTERNAMN = "Tass";

   public static PatchDataRequest newPatchDataRequest()
   {
      var updateErsattning = new UpdateErsattning();
      updateErsattning.setErsattningId(UUID.fromString(ERSATTNINGSID));
      updateErsattning.setYrkandestatus("16bd3d95-8fad-4f9c-a87a-ba146db55c9b");

      var beslut = new Beslut();
      beslut.setAvslutstyp("b1364919-6002-4e37-a759-556bd2ec4bfd");
      beslut.setBeslutstyp("e44d5e15-5231-4857-b992-5b8d9c34e36f");
      beslut.setBeslutsutfall("f011934d-d05e-404d-95ab-24571eec241b");

      PatchDataRequest patchDataRequest = new PatchDataRequest();
      patchDataRequest.addErsattningarItem(updateErsattning);
      patchDataRequest.setBeslut(beslut);
      return patchDataRequest;
   }
}
