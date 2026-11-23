package hu.mik.pte.prog5.mozi.model.nezet;

import hu.mik.pte.prog5.mozi.model.JegyStatus;

import java.time.LocalDateTime;

// Egy ugyfel jegye a film es a vetites adataival egyutt
public record SajatJegy(
        Long jegyId,
        Long vetitesId,
        String filmCim,
        String terem,
        LocalDateTime kezdesIdo,
        Integer szekSzam,
        Integer ar,
        JegyStatus status) {
}
