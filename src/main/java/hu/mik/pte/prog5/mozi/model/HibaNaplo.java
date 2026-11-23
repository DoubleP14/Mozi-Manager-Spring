package hu.mik.pte.prog5.mozi.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class HibaNaplo {

    @Id
    private Long hibaId;

    private String hibaKod;

    private String hibaUzenet;

    private String eljarasNeve;

    // az adatbazis default erteke (now()) tolti ki
    @ReadOnlyProperty
    private LocalDateTime idopont;

    private String felhasznalo;

    public HibaNaplo(String hibaKod, String hibaUzenet, String eljarasNeve, String felhasznalo) {
        this.hibaKod = hibaKod;
        this.hibaUzenet = hibaUzenet;
        this.eljarasNeve = eljarasNeve;
        this.felhasznalo = felhasznalo;
    }
}
