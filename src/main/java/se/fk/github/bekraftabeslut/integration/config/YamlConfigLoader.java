package se.fk.github.bekraftabeslut.integration.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class YamlConfigLoader
{

   private YamlConfigLoader()
   {
   } // utility class

   public static <T> T loadFromFile(Path path, Class<T> clazz)
   {
      if (!Files.exists(path))
      {
         throw new IllegalStateException("YAML config not found: " + path);
      }

      LoaderOptions loaderOptions = new LoaderOptions();
      Constructor constructor = new Constructor(clazz, loaderOptions);
      Yaml yaml = new Yaml(constructor);

      try (InputStream inputStream = Files.newInputStream(path))
      {
         return yaml.load(inputStream);
      }
      catch (Exception e)
      {
         throw new RuntimeException("Failed to load YAML config: " + path, e);
      }
   }

   public static <T> T loadFromClasspath(String resource, Class<T> type)
   {
      try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource))
      {
         if (is == null)
         {
            throw new IllegalStateException("YAML config not found on classpath: " + resource);
         }
         return new Yaml().loadAs(is, type);
      }
      catch (IOException e)
      {
         throw new RuntimeException("Failed to load YAML config: " + resource, e);
      }
   }
}
