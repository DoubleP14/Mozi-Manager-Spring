package hu.mik.pte.prog5.mozi.service;

import hu.mik.pte.prog5.mozi.model.HibaNaplo;
import hu.mik.pte.prog5.mozi.repository.HibaNaploRepository;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Az eredeti mozi_manager.log_error megfeleloje.
 * Az Oracle valtozat PRAGMA AUTONOMOUS_TRANSACTION-nel irt a naploba. PostgreSQL-ben ilyen nincs,
 * ezert a REQUIRES_NEW sajat tranzakciot nyit: a bejegyzes akkor is megmarad,
 * ha a hivo tranzakcio (pl. a jegyvasarlas) visszagordul.
 */
@Service
public class HibaNaploService {

    private static final int MAX_HOSSZ = 4000;

    private final HibaNaploRepository hibaNaploRepository;
    private final AuditorAware<String> auditorAware;

    public HibaNaploService(HibaNaploRepository hibaNaploRepository, AuditorAware<String> auditorAware) {
        this.hibaNaploRepository = hibaNaploRepository;
        this.auditorAware = auditorAware;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void naploz(String hibaKod, String uzenet, String eljarasNeve) {
        String rovidUzenet = uzenet != null && uzenet.length() > MAX_HOSSZ ? uzenet.substring(0, MAX_HOSSZ) : uzenet;
        String felhasznalo = this.auditorAware.getCurrentAuditor().orElse(null);
        this.hibaNaploRepository.save(new HibaNaplo(hibaKod, rovidUzenet, eljarasNeve, felhasznalo));
    }
}
