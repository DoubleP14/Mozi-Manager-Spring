package hu.mik.pte.prog5.mozi.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@NoArgsConstructor
public class Jegy extends AuditaltEntitas {

    @Id
    private Long jegyId;

    private Long vetitesId;

    private Long ugyfelId;

    private Integer szekSzam;

    private Integer ar;

    private JegyStatus status;

    public Jegy(Long vetitesId, Long ugyfelId, Integer szekSzam, Integer ar, JegyStatus status) {
        this.vetitesId = vetitesId;
        this.ugyfelId = ugyfelId;
        this.szekSzam = szekSzam;
        this.ar = ar;
        this.status = status;
    }
}
