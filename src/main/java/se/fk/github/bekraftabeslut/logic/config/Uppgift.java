package se.fk.github.bekraftabeslut.logic.config;

@SuppressWarnings("unused")
public class Uppgift
{
   private String namn;
   private String beskrivning;
   private String verksamhetslogik;
   private String roll;

   private String path;

   public Uppgift()
   {
      // required by SnakeYAML
   }

   public String getNamn()
   {
      return namn;
   }

   public void setNamn(String namn)
   {
      this.namn = namn;
   }

   public String getBeskrivning()
   {
      return beskrivning;
   }

   public void setBeskrivning(String beskrivning)
   {
      this.beskrivning = beskrivning;
   }

   public String getVerksamhetslogik()
   {
      return verksamhetslogik;
   }

   public void setVerksamhetslogik(String verksamhetslogik)
   {
      this.verksamhetslogik = verksamhetslogik;
   }

   public String getRoll()
   {
      return roll;
   }

   public void setRoll(String roll)
   {
      this.roll = roll;
   }

   public String getPath()
   {
      return path;
   }

   public void setPath(String path)
   {
      this.path = path;
   }

}
