package hu.mik.pte.prog5.mozi.service;

import lombok.Getter;

/**
 * Uzleti szabaly megsertese, pl. foglalt szek.
 * A kod a forditasi kulcs, az uzenet a magyar alapszoveg.
 * A tipus alapjan donti el a REST API, milyen HTTP statuszt ad; a service nem tud a HTTP-rol.
 */
@Getter
public class UzletiSzabalyException extends RuntimeException {

    public enum Tipus {
        ERVENYTELEN,     // hibas keres, pl. elmult vetites
        NEM_TALALHATO,   // nem letezo vetites, jegy
        UTKOZES,         // foglalt szek, foglalt e-mail cim
        TILTOTT          // mas jegye
    }

    private final Tipus tipus;
    private final String kod;

    public UzletiSzabalyException(Tipus tipus, String kod, String uzenet) {
        super(uzenet);
        this.tipus = tipus;
        this.kod = kod;
    }
}
