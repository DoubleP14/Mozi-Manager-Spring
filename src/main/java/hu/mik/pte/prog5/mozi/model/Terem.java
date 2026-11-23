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
public class Terem extends AuditaltEntitas {

    @Id
    private Long teremId;

    @NotBlank
    @Size(max = 50)
    private String nev;

    @NotNull
    @Min(1)
    private Integer kapacitas;

    public Terem(String nev, Integer kapacitas) {
        this.nev = nev;
        this.kapacitas = kapacitas;
    }
}
