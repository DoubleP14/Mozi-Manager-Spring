package hu.mik.pte.prog5.mozi.api.dto;

import hu.mik.pte.prog5.mozi.model.Szerep;
import hu.mik.pte.prog5.mozi.model.Ugyfel;

// A bejelentkezett felhasznalo adatai a feluletnek. A jelszo hash szandekosan nincs benne.
public record FelhasznaloAdat(Long ugyfelId, String nev, String email, Szerep szerep, Integer torzsvasarloiPontok) {

    public static FelhasznaloAdat of(Ugyfel ugyfel) {
        return new FelhasznaloAdat(ugyfel.getUgyfelId(), ugyfel.getNev(), ugyfel.getEmail(),
                ugyfel.getSzerep(), ugyfel.getTorzsvasarloiPontok());
    }
}
