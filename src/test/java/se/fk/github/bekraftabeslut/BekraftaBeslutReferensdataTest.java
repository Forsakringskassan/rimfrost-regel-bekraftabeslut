package se.fk.github.bekraftabeslut;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;
import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Referensdata;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.fk.github.bekraftabeslut.BekraftaBeslutRestMock.sendGetAvslutstypBekraftaBeslut;
import static se.fk.github.bekraftabeslut.BekraftaBeslutRestMock.sendGetBeslutstypBekraftaBeslut;
import static se.fk.github.bekraftabeslut.BekraftaBeslutRestMock.sendGetBeslutsutfallstypBekraftaBeslut;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockBekraftaBeslut.class)
})
class BekraftaBeslutReferensdataTest extends AbstractRegelManuellTest
{
   @Test
   void get_avslutstyp_should_return_expected_ids()
   {
      var avslutstyper = sendGetAvslutstypBekraftaBeslut();
      assertEquals(3, avslutstyper.size());
      assertEquals(List.of("dcfb24d5-b0a9-4519-86e5-aece09d85ca9",
            "01ea4c9f-50da-4b99-8fc3-6aee6f4b506d", "b1364919-6002-4e37-a759-556bd2ec4bfd"),
            avslutstyper.stream().map(Referensdata::getId).toList());
   }

   @Test
   void get_beslutstyp_should_return_expected_ids()
   {
      var beslutstyper = sendGetBeslutstypBekraftaBeslut();
      assertEquals(3, beslutstyper.size());
      assertEquals(List.of("c61bf2cd-3f11-4629-8bc2-6ab9ca72666a",
            "e44d5e15-5231-4857-b992-5b8d9c34e36f", "04ece3fc-9015-412e-b48f-eb3ac6d6c846"),
            beslutstyper.stream().map(Referensdata::getId).toList());
   }

   @Test
   void get_beslutsutfallstyp_should_return_expected_ids()
   {
      var beslutsutfallstyper = sendGetBeslutsutfallstypBekraftaBeslut();
      assertEquals(3, beslutsutfallstyper.size());
      assertEquals(List.of("f011934d-d05e-404d-95ab-24571eec241b",
            "1429cc69-1e9d-49be-be41-8e5e4e05a0f9", "5922e22f-4872-4757-938c-5925ef650742"),
            beslutsutfallstyper.stream().map(Referensdata::getId).toList());
   }
}
