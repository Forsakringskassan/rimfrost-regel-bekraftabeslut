package se.fk.github.bekraftabeslut.integration.arbetsgivare.dto;

import java.time.LocalDate;

import org.immutables.value.Value;

import jakarta.annotation.Nullable;

@Value.Immutable
public interface ArbetsgivareResponse
{

   String organisationsnummer();

   String organisationsnamn();

   int arbetstidProcent();

   int loneSumma();

   LocalDate anstallningsdag();

   @Nullable
   LocalDate sistaAnstallningsdag();

   LocalDate lonFrom();

   @Nullable
   LocalDate lonTom();
}
