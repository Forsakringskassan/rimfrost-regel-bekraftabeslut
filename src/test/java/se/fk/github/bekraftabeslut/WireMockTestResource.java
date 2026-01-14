package se.fk.github.bekraftabeslut;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class WireMockTestResource implements QuarkusTestResourceLifecycleManager
{

   private WireMockServer server;

   @Override
   public Map<String, String> start()
   {
      server = new WireMockServer(options().dynamicPort());
      server.start();

      // valfritt men bra: börja alltid rent
      server.resetAll();

      return Map.of(
            // sätt din apps config till WireMocks baseUrl
            "kundbehovsflode.base-url", server.baseUrl());
   }

   @Override
   public void stop()
   {
      if (server != null)
      {
         server.stop();
      }
   }

   // Om du vill kunna stubba från tester:
   public WireMockServer getServer()
   {
      return server;
   }
}