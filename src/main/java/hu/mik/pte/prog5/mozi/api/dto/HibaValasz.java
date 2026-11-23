package hu.mik.pte.prog5.mozi.api.dto;

import java.util.Map;

// Egyseges hibavalasz a feluletnek: kod (forditasi kulcs), szoveg, es validacional a hibas mezok
public record HibaValasz(String kod, String uzenet, Map<String, String> mezok) {

    public HibaValasz(String kod, String uzenet) {
        this(kod, uzenet, Map.of());
    }
}
