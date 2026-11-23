package hu.mik.pte.prog5.mozi.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisztracioKeres(
        @NotBlank(message = "A név kötelező.")
        @Size(max = 100, message = "A név legfeljebb 100 karakter lehet.")
        String nev,

        @NotBlank(message = "Az e-mail cím kötelező.")
        @Email(message = "Érvénytelen e-mail cím.")
        @Size(max = 100, message = "Az e-mail cím legfeljebb 100 karakter lehet.")
        String email,

        // a BCrypt legfeljebb 72 bajtot vesz figyelembe
        @NotBlank(message = "A jelszó kötelező.")
        @Size(min = 6, max = 72, message = "A jelszó 6 és 72 karakter között legyen.")
        String jelszo) {
}
