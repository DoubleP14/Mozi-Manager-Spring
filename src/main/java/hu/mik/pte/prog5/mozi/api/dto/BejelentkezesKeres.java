package hu.mik.pte.prog5.mozi.api.dto;

import jakarta.validation.constraints.NotBlank;

public record BejelentkezesKeres(
        @NotBlank(message = "Az e-mail cím kötelező.") String email,
        @NotBlank(message = "A jelszó kötelező.") String jelszo) {
}
