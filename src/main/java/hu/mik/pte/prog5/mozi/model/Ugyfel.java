package hu.mik.pte.prog5.mozi.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;

@Getter
@Setter
@NoArgsConstructor
public class Ugyfel extends AuditaltEntitas {

    @Id
    private Long ugyfelId;

    @NotBlank
    @Size(max = 100)
    private String nev;

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    @NotBlank
    private String jelszoHash;

    @NotNull
    private Szerep szerep = Szerep.UGYFEL;

    // A pontokat a pontgyujtes trigger modositja. Csak olvashato, igy egy regebben
    // betoltott ugyfel mentese nem irhatja vissza az elavult pontszamot.
    @ReadOnlyProperty
    private Integer torzsvasarloiPontok;

    public Ugyfel(String nev, String email, String jelszoHash, Szerep szerep) {
        this.nev = nev;
        this.email = email;
        this.jelszoHash = jelszoHash;
        this.szerep = szerep;
    }
}
