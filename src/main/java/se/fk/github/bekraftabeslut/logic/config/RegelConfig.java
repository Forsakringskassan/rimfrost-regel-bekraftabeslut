package se.fk.github.bekraftabeslut.logic.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class RegelConfig
{

   private Uppgift uppgift;

   // REQUIRED by SnakeYAML
   @SuppressWarnings("unused")
   public RegelConfig()
   {
   }

   @SuppressWarnings("unused")
   @SuppressFBWarnings("EI_EXPOSE_REP")
   public RegelConfig(Uppgift uppgift)
   {
      this.uppgift = uppgift;
   }

   @SuppressFBWarnings("EI_EXPOSE_REP")
   public Uppgift getUppgift()
   {
      return uppgift;
   }

   @SuppressWarnings("unused")
   @SuppressFBWarnings("EI_EXPOSE_REP")
   public void setUppgift(Uppgift uppgift)
   {
      this.uppgift = uppgift;
   }
}
