package hu.mik.pte.prog5.mozi.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@NoArgsConstructor
public class Kategoria extends AuditaltEntitas {

    // Az oszlop neve kategoria_id, a Spring Data JDBC a mezonevbol (kategoriaId) kepzi
    @Id
    private Long kategoriaId;

    @NotBlank
    @Size(max = 50)
    private String megnevezes;

    public Kategoria(String megnevezes) {
        this.megnevezes = megnevezes;
    }
}
