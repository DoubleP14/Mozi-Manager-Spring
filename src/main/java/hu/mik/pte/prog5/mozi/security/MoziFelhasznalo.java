package hu.mik.pte.prog5.mozi.security;

import hu.mik.pte.prog5.mozi.model.Ugyfel;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

/**
 * A bejelentkezett felhasznalo. A Spring Security User osztalyat egesziti ki az ugyfel azonositojaval,
 * igy a controllerek @AuthenticationPrincipal-lal kozvetlenul megkapjak, ki vasarol.
 */
@Getter
public class MoziFelhasznalo extends User {

    private final Long ugyfelId;
    private final String nev;

    public MoziFelhasznalo(Ugyfel ugyfel) {
        // A felhasznalonev az e-mail cim, a szerepbol ROLE_UGYFEL vagy ROLE_ADMIN jog lesz
        super(ugyfel.getEmail(), ugyfel.getJelszoHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + ugyfel.getSzerep().name())));
        this.ugyfelId = ugyfel.getUgyfelId();
        this.nev = ugyfel.getNev();
    }
}
