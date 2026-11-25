package hu.mik.pte.prog5.mozi.api.dto;

import hu.mik.pte.prog5.mozi.model.nezet.MusorElem;

import java.util.List;

/**
 * Egy vetites a szekvalasztohoz: az adatai, a foglalt szekek, es hogy mennyibe kerul egy jegy.
 * Az ar bejelentkezve a sajat pontokbol szamolodik, egyebkent az alapar.
 */
public record VetitesReszletek(MusorElem vetites, List<Integer> foglaltSzekek, int egysegar) {
}
