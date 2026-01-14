package se.fk.github.bekraftabeslut.integration.kafka.dto;

import java.util.UUID;

import org.immutables.value.Value;

@Value.Immutable
public interface OulMessageRequest
{

   UUID kundbehovsflodeId();

   String kundbehov();

   String regel();

   String beskrivning();

   String verksamhetslogik();

   String roll();

   String url();

}
