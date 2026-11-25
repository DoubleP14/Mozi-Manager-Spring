package hu.mik.pte.prog5.mozi.model.nezet;

import java.time.LocalDateTime;

// A hiba_naplo tabla egy sora az admin feluletnek
public record HibaNaploSor(Long hibaId, LocalDateTime idopont, String hibaKod, String hibaUzenet,
                           String eljarasNeve, String felhasznalo) {
}
