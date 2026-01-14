package se.fk.github.bekraftabeslut.integration.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.bekraftabeslut.logic.config.RegelConfig;

import java.nio.file.Path;

@SuppressWarnings("unused")
@ApplicationScoped
@Startup
public class RegelConfigProviderYaml implements RegelConfigProvider
{

   private static final String ENV_CONFIG_PATH = "REGEL_CONFIG_PATH";
   private static final String DEFAULT_CONFIG_PATH = "src/main/resources/config.yaml"; // local default

   private RegelConfig config;

   @PostConstruct
   void init()
   {
      String configPath = System.getenv(ENV_CONFIG_PATH);
      if (configPath == null || configPath.isBlank())
      {
         this.config = YamlConfigLoader.loadFromClasspath(
               "config.yaml",
               RegelConfig.class);
      }
      else
      {
         this.config = YamlConfigLoader.loadFromFile(Path.of(configPath), RegelConfig.class);
      }
   }

   @SuppressFBWarnings("EI_EXPOSE_REP")
   @Override
   public RegelConfig getConfig()
   {
      return config;
   }
}
