package hu.mik.pte.prog5.mozi.service;

import lombok.Getter;

/**
 * Uzleti szabaly megsertese, pl. foglalt szek. A kod kesobb a forditasi kulcs lesz,
 * az uzenet a magyar alapszoveg.
 */
@Getter
public class UzletiSzabalyException extends RuntimeException {

    private final String kod;

    public UzletiSzabalyException(String kod, String uzenet) {
        super(uzenet);
        this.kod = kod;
    }
}
