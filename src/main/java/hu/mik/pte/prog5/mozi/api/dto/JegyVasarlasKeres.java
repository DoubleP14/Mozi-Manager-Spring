package hu.mik.pte.prog5.mozi.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record JegyVasarlasKeres(
        @NotNull(message = "A vetítés kötelező.") Long vetitesId,
        @NotEmpty(message = "Válassz legalább egy széket.") List<@NotNull Integer> szekek) {
}
