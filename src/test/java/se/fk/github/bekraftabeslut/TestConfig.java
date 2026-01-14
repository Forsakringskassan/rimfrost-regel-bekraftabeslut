package se.fk.github.bekraftabeslut;

import java.io.InputStream;
import java.util.Properties;

public class TestConfig
{
   private static final Properties props = new Properties();

   static
   {
      try (InputStream in = TestConfig.class.getResourceAsStream("/test.properties"))
      {
         if (in == null)
         {
            throw new IllegalStateException("Could not find /test.properties on classpath");
         }
         props.load(in);
      }
      catch (Exception e)
      {
         throw new RuntimeException("Failed to load test.properties", e);
      }
   }

   public static String get(String key)
   {
      // Låt system properties vinna (bra ihop med QuarkusTestResource overrides)
      String sys = System.getProperty(key);
      if (sys != null && !sys.isBlank())
      {
         return sys;
      }

      String val = props.getProperty(key);
      if (val == null)
      {
         throw new IllegalArgumentException("Missing key in test.properties (or -D): " + key);
      }
      return val;
   }

   public static int getInt(String key)
   {
      return Integer.parseInt(get(key));
   }
}
