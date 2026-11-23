package hu.mik.pte.prog5.mozi.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@NoArgsConstructor
public class Film extends AuditaltEntitas {

    @Id
    private Long filmId;

    @NotBlank
    @Size(max = 100)
    private String cim;

    @NotNull
    @Min(1)
    private Integer hosszPerc;

    // Masik aggregatumra csak az azonositojaval hivatkozunk
    @NotNull
    private Long kategoriaId;

    public Film(String cim, Integer hosszPerc, Long kategoriaId) {
        this.cim = cim;
        this.hosszPerc = hosszPerc;
        this.kategoriaId = kategoriaId;
    }
}
