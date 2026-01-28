package se.fk.github.bekraftabeslut.integration.kundbehovsflode.dto;

import jakarta.annotation.Nullable;
import org.immutables.value.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value.Immutable
public interface UpdateKundbehovsflodeLagrum
{

   UUID id();

   String version();

   OffsetDateTime giltigFrom();

   @Nullable
   OffsetDateTime giltigTom();

   String forfattning();

   String kapitel();

   String paragraf();

   String stycke();

   String punkt();
}
