package hu.mik.pte.prog5.mozi.model;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class Vetites extends AuditaltEntitas {

    @Id
    private Long vetitesId;

    @NotNull
    private Long filmId;

    @NotNull
    private Long teremId;

    @NotNull
    private LocalDateTime kezdesIdo;

    public Vetites(Long filmId, Long teremId, LocalDateTime kezdesIdo) {
        this.filmId = filmId;
        this.teremId = teremId;
        this.kezdesIdo = kezdesIdo;
    }
}
