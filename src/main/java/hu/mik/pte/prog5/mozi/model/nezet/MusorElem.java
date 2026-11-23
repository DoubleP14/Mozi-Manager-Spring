package hu.mik.pte.prog5.mozi.model.nezet;

import java.time.LocalDateTime;

// A v_musor nezet egy sora
public record MusorElem(
        Long vetitesId,
        Long filmId,
        String filmCim,
        String kategoria,
        Long teremId,
        String terem,
        Integer kapacitas,
        LocalDateTime kezdesIdo,
        Integer hosszPerc,
        Integer szabadHelyek) {
}
