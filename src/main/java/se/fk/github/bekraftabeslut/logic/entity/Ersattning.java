package se.fk.github.bekraftabeslut.logic.entity;

import se.fk.rimfrost.regel.bekraftabeslut.openapi.jaxrsspec.controllers.generatedsource.model.Beslutsutfall;

public record Ersattning(String ersattningstyp,int omfattningProcent,int belopp,int berakningsgrund,Beslutsutfall beslutsutfall){}
