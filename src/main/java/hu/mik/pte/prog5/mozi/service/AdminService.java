package hu.mik.pte.prog5.mozi.service;

import hu.mik.pte.prog5.mozi.model.Film;
import hu.mik.pte.prog5.mozi.model.Kategoria;
import hu.mik.pte.prog5.mozi.model.Terem;
import hu.mik.pte.prog5.mozi.model.Vetites;
import hu.mik.pte.prog5.mozi.model.nezet.BevetelSor;
import hu.mik.pte.prog5.mozi.model.nezet.HibaNaploSor;
import hu.mik.pte.prog5.mozi.model.nezet.MusorElem;
import hu.mik.pte.prog5.mozi.repository.FilmRepository;
import hu.mik.pte.prog5.mozi.repository.JegyRepository;
import hu.mik.pte.prog5.mozi.repository.KategoriaRepository;
import hu.mik.pte.prog5.mozi.repository.LekerdezesRepository;
import hu.mik.pte.prog5.mozi.repository.TeremRepository;
import hu.mik.pte.prog5.mozi.repository.VetitesRepository;
import hu.mik.pte.prog5.mozi.service.UzletiSzabalyException.Tipus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Az admin felulet muveletei: torzsadatok (kategoria, terem, film), vetitesek, statisztika.
 * Az osztalyon levo @PreAuthorize minden metodusra vonatkozik: ha a felulet vedelme
 * valahol hianyozna, a service akkor is csak ADMIN-nak engedi.
 */
@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminService {

    // Ket vetites kozott ennyi ido kell a terem takaritasara
    public static final int TAKARITAS_PERC = 15;

    private final KategoriaRepository kategoriaRepository;
    private final TeremRepository teremRepository;
    private final FilmRepository filmRepository;
    private final VetitesRepository vetitesRepository;
    private final JegyRepository jegyRepository;
    private final LekerdezesRepository lekerdezesRepository;

    public AdminService(KategoriaRepository kategoriaRepository, TeremRepository teremRepository,
                        FilmRepository filmRepository, VetitesRepository vetitesRepository,
                        JegyRepository jegyRepository, LekerdezesRepository lekerdezesRepository) {
        this.kategoriaRepository = kategoriaRepository;
        this.teremRepository = teremRepository;
        this.filmRepository = filmRepository;
        this.vetitesRepository = vetitesRepository;
        this.jegyRepository = jegyRepository;
        this.lekerdezesRepository = lekerdezesRepository;
    }

    // ===== Kategoria =====

    public List<Kategoria> kategoriak() {
        return this.kategoriaRepository.findAll().stream()
                .sorted(Comparator.comparing(Kategoria::getMegnevezes))
                .toList();
    }

    @Transactional
    public Kategoria kategoriaMentes(Kategoria kategoria) {
        try {
            return this.kategoriaRepository.save(kategoria);
        } catch (DuplicateKeyException e) {
            throw new UzletiSzabalyException(Tipus.UTKOZES, "kategoria.letezik", "Ilyen nevű kategória már van.");
        }
    }

    @Transactional
    public void kategoriaTorles(long kategoriaId) {
        torles(() -> this.kategoriaRepository.deleteById(kategoriaId),
                "kategoria.hasznalatban", "A kategória nem törölhető, mert film tartozik hozzá.");
    }

    // ===== Terem =====

    public List<Terem> termek() {
        return this.teremRepository.findAll().stream()
                .sorted(Comparator.comparing(Terem::getNev))
                .toList();
    }

    @Transactional
    public Terem teremMentes(Terem terem) {
        // A kapacitas nem csokkenhet a mar eladott jegyek szekszama ala
        if (terem.getTeremId() != null) {
            int maxSzek = this.jegyRepository.maxEladottSzek(terem.getTeremId());
            if (terem.getKapacitas() < maxSzek) {
                throw new UzletiSzabalyException(Tipus.ERVENYTELEN, "terem.kapacitas.kicsi",
                        "A kapacitás nem lehet kevesebb " + maxSzek + "-nál, mert erre a székre már van jegy.");
            }
        }
        try {
            return this.teremRepository.save(terem);
        } catch (DuplicateKeyException e) {
            throw new UzletiSzabalyException(Tipus.UTKOZES, "terem.letezik", "Ilyen nevű terem már van.");
        }
    }

    @Transactional
    public void teremTorles(long teremId) {
        torles(() -> this.teremRepository.deleteById(teremId),
                "terem.hasznalatban", "A terem nem törölhető, mert vetítés tartozik hozzá.");
    }

    // ===== Film =====

    public List<Film> filmek() {
        return this.filmRepository.findAll().stream()
                .sorted(Comparator.comparing(Film::getCim))
                .toList();
    }

    @Transactional
    public Film filmMentes(Film film) {
        return this.filmRepository.save(film);
    }

    @Transactional
    public void filmTorles(long filmId) {
        torles(() -> this.filmRepository.deleteById(filmId),
                "film.hasznalatban", "A film nem törölhető, mert vetítés tartozik hozzá.");
    }

    // ===== Vetites =====

    // A szerkesztheto vetitesek, a legujabb elol
    public List<Vetites> vetitesek() {
        return this.vetitesRepository.findAll().stream()
                .sorted(Comparator.comparing(Vetites::getKezdesIdo).reversed())
                .toList();
    }

    // A v_musor nezet sorai: ebbol jon a szabad helyek szama
    public List<MusorElem> musor() {
        return this.lekerdezesRepository.findTeljesMusor();
    }

    public Optional<Vetites> vetites(long vetitesId) {
        return this.vetitesRepository.findById(vetitesId);
    }

    /**
     * Uzleti szabalyok:
     *  - uj vetites nem lehet a multban
     *  - a teremben nem lehet idoben atfedo masik vetites (film hossza + takaritas)
     *  - ha mar van eladott jegy, a terem nem csereltheto (a szekszamok a regi teremre szolnak)
     */
    @Transactional
    public Vetites vetitesMentes(Vetites vetites) {
        if (vetites.getVetitesId() == null && !vetites.getKezdesIdo().isAfter(LocalDateTime.now())) {
            throw new UzletiSzabalyException(Tipus.ERVENYTELEN, "vetites.multban",
                    "Új vetítés nem lehet a múltban.");
        }

        Film film = this.filmRepository.findById(vetites.getFilmId())
                .orElseThrow(() -> new UzletiSzabalyException(Tipus.NEM_TALALHATO, "film.nincs", "Nem létező film."));
        LocalDateTime veg = vetites.getKezdesIdo().plusMinutes(film.getHosszPerc() + TAKARITAS_PERC);
        if (this.vetitesRepository.vanAtfedoVetites(vetites.getTeremId(), vetites.getVetitesId(),
                vetites.getKezdesIdo(), veg, TAKARITAS_PERC)) {
            throw new UzletiSzabalyException(Tipus.UTKOZES, "vetites.atfedes",
                    "Ebben a teremben ekkor már van vetítés.");
        }

        if (vetites.getVetitesId() != null) {
            Vetites regi = this.vetitesRepository.findById(vetites.getVetitesId())
                    .orElseThrow(() -> new UzletiSzabalyException(Tipus.NEM_TALALHATO, "jegy.vetites.nincs",
                            "Nem létező vetítés."));
            if (!regi.getTeremId().equals(vetites.getTeremId())
                    && this.jegyRepository.countAktivJegyek(vetites.getVetitesId()) > 0) {
                throw new UzletiSzabalyException(Tipus.ERVENYTELEN, "vetites.terem.jegyek",
                        "A terem nem módosítható, mert erre a vetítésre már van eladott jegy.");
            }
        }
        return this.vetitesRepository.save(vetites);
    }

    @Transactional
    public void vetitesTorles(long vetitesId) {
        torles(() -> this.vetitesRepository.deleteById(vetitesId),
                "vetites.hasznalatban", "A vetítés nem törölhető, mert van hozzá jegy.");
    }

    // ===== Statisztika =====

    public List<BevetelSor> bevetelek() {
        return this.lekerdezesRepository.findBevetelek();
    }

    public List<HibaNaploSor> hibak() {
        return this.lekerdezesRepository.findLegutobbiHibak(100);
    }

    // Az idegen kulcs megsertese (a rekordra mas tabla hivatkozik) uzleti hibakent jelenik meg
    private static void torles(Runnable muvelet, String kod, String uzenet) {
        try {
            muvelet.run();
        } catch (DataIntegrityViolationException e) {
            throw new UzletiSzabalyException(Tipus.UTKOZES, kod, uzenet);
        }
    }
}
