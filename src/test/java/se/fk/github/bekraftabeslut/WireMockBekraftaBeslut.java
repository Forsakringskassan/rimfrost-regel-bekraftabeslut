package se.fk.github.bekraftabeslut;

import com.github.tomakehurst.wiremock.WireMockServer;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import java.util.HashMap;
import java.util.Map;

public class WireMockBekraftaBeslut extends WireMockRegelManuell
{

   @Override
   protected Map<String, String> wiremockMapping(WireMockServer server)
   {
      Map<String, String> map = new HashMap<>(super.wiremockMapping(server));
      map.put("folkbokford.api.base-url", server.baseUrl());
      map.put("arbetsgivare.api.base-url", server.baseUrl());
      map.put("referensdata.api.base-url", server.baseUrl());
      map.put("oul.api.base-url", server.baseUrl());
      return map;
   }
}
