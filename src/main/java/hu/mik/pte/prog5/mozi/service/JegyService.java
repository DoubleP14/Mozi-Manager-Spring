package hu.mik.pte.prog5.mozi.service;

import hu.mik.pte.prog5.mozi.model.Jegy;
import hu.mik.pte.prog5.mozi.model.JegyStatus;
import hu.mik.pte.prog5.mozi.model.Terem;
import hu.mik.pte.prog5.mozi.model.Ugyfel;
import hu.mik.pte.prog5.mozi.model.Vetites;
import hu.mik.pte.prog5.mozi.model.nezet.SajatJegy;
import hu.mik.pte.prog5.mozi.repository.JegyRepository;
import hu.mik.pte.prog5.mozi.repository.LekerdezesRepository;
import hu.mik.pte.prog5.mozi.repository.TeremRepository;
import hu.mik.pte.prog5.mozi.repository.UgyfelRepository;
import hu.mik.pte.prog5.mozi.repository.VetitesRepository;
import hu.mik.pte.prog5.mozi.service.UzletiSzabalyException.Tipus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Jegyvasarlas es lemondas. Az eredeti mozi_manager.jegy_vasarlas eljaras Java valtozata.
 */
@Service
public class JegyService {

    public static final int ALAP_AR = 2500;
    public static final int KEDVEZMENYES_AR = 2000;
    // Ennel tobb pont felett jar a kedvezmenyes ar, mint az eredeti eljarasban
    public static final int KEDVEZMENY_PONTHATAR = 50;
    public static final int MAX_JEGY_EGYSZERRE = 10;

    private static final String VASARLAS = "jegy_vasarlas";
    private static final String LEMONDAS = "jegy_lemondas";

    private final JegyRepository jegyRepository;
    private final VetitesRepository vetitesRepository;
    private final TeremRepository teremRepository;
    private final UgyfelRepository ugyfelRepository;
    private final LekerdezesRepository lekerdezesRepository;
    private final HibaNaploService hibaNaploService;

    public JegyService(JegyRepository jegyRepository, VetitesRepository vetitesRepository,
                       TeremRepository teremRepository, UgyfelRepository ugyfelRepository,
                       LekerdezesRepository lekerdezesRepository, HibaNaploService hibaNaploService) {
        this.jegyRepository = jegyRepository;
        this.vetitesRepository = vetitesRepository;
        this.teremRepository = teremRepository;
        this.ugyfelRepository = ugyfelRepository;
        this.lekerdezesRepository = lekerdezesRepository;
        this.hibaNaploService = hibaNaploService;
    }

    public List<Integer> foglaltSzekek(long vetitesId) {
        return this.jegyRepository.findFoglaltSzekek(vetitesId);
    }

    public List<SajatJegy> sajatJegyek(long ugyfelId) {
        return this.lekerdezesRepository.findSajatJegyek(ugyfelId);
    }

    // Az ar a torzsvasarloi pontoktol fugg: ennel tobb pont felett kedvezmenyes
    public static int egysegar(int torzsvasarloiPontok) {
        return torzsvasarloiPontok > KEDVEZMENY_PONTHATAR ? KEDVEZMENYES_AR : ALAP_AR;
    }

    // Egy szek megvasarlasa (a tesztek es az egyszeru esetek kedveert)
    @Transactional
    public Jegy vasarlas(long vetitesId, long ugyfelId, int szekSzam) {
        return this.vasarlas(vetitesId, ugyfelId, List.of(szekSzam)).getFirst();
    }

    /**
     * Tobb szek egy tranzakcioban: vagy mindegyik jegy megszuletik, vagy egyik sem.
     * A hibanaplo bejegyzese viszont megmarad (lasd HibaNaploService).
     * Az ar a vasarlas elejen dol el, igy egy vasarlason belul minden jegy ugyanannyiba kerul,
     * akkor is, ha kozben a pontgyujtes atlepi a hatart.
     */
    @Transactional
    public List<Jegy> vasarlas(long vetitesId, long ugyfelId, List<Integer> szekek) {
        try {
            if (szekek == null || szekek.isEmpty()) {
                throw new UzletiSzabalyException(Tipus.ERVENYTELEN, "jegy.szek.ures", "Válassz legalább egy széket.");
            }
            if (szekek.size() > MAX_JEGY_EGYSZERRE) {
                throw new UzletiSzabalyException(Tipus.ERVENYTELEN, "jegy.tul.sok",
                        "Egyszerre legfeljebb " + MAX_JEGY_EGYSZERRE + " jegy vehető.");
            }
            if (new HashSet<>(szekek).size() != szekek.size()) {
                throw new UzletiSzabalyException(Tipus.ERVENYTELEN, "jegy.szek.tobbszor", "Egy szék csak egyszer szerepelhet.");
            }

            // 1. a vetites zarolasa, igy erre a vetitesre egyszerre csak egy vasarlas fut
            Vetites vetites = this.vetitesRepository.findByIdForUpdate(vetitesId)
                    .orElseThrow(() -> new UzletiSzabalyException(Tipus.NEM_TALALHATO, "jegy.vetites.nincs", "Nem létező vetítés."));
            if (!vetites.getKezdesIdo().isAfter(LocalDateTime.now())) {
                throw new UzletiSzabalyException(Tipus.ERVENYTELEN, "jegy.vetites.elmult", "Erre a vetítésre már nem lehet jegyet venni.");
            }

            // 2. minden szek letezik-e es szabad-e
            Terem terem = this.teremRepository.findById(vetites.getTeremId()).orElseThrow();
            for (int szekSzam : szekek) {
                if (szekSzam < 1 || szekSzam > terem.getKapacitas()) {
                    throw new UzletiSzabalyException(Tipus.ERVENYTELEN, "jegy.szek.ervenytelen", "Nem létező szék: " + szekSzam + ".");
                }
                if (this.jegyRepository.isSzekFoglalt(vetitesId, szekSzam)) {
                    throw new UzletiSzabalyException(Tipus.UTKOZES, "jegy.szek.foglalt", "A(z) " + szekSzam + ". szék már foglalt!");
                }
            }

            // 3. az ar a torzsvasarloi pontoktol fugg
            Ugyfel ugyfel = this.ugyfelRepository.findById(ugyfelId)
                    .orElseThrow(() -> new UzletiSzabalyException(Tipus.NEM_TALALHATO, "jegy.ugyfel.nincs", "Nem létező ügyfél."));
            int ar = egysegar(ugyfel.getTorzsvasarloiPontok());

            // 4. mentes; a pontokat a trg_pontgyujtes trigger irja jova
            List<Jegy> jegyek = new ArrayList<>();
            for (int szekSzam : szekek) {
                jegyek.add(this.jegyRepository.save(new Jegy(vetitesId, ugyfelId, szekSzam, ar, JegyStatus.FIZETVE)));
            }
            return jegyek;

        } catch (UzletiSzabalyException e) {
            this.hibaNaploService.naploz(e.getKod(), e.getMessage(), VASARLAS);
            throw e;
        } catch (DuplicateKeyException e) {
            // Az ux_jegy_vetites_szek index vedi a szeket, ha az ellenorzes es a mentes kozott valaki megelozott
            this.hibaNaploService.naploz("jegy.szek.foglalt", e.getMessage(), VASARLAS);
            throw new UzletiSzabalyException(Tipus.UTKOZES, "jegy.szek.foglalt", "A szék már foglalt!");
        } catch (RuntimeException e) {
            this.hibaNaploService.naploz(e.getClass().getSimpleName(), e.getMessage(), VASARLAS);
            throw e;
        }
    }

    /**
     * Csak a sajat, meg el nem kezdodott vetitesre szolo jegy mondhato le.
     * A jegy nem torlodik, csak TOROLT lesz, igy a szek ujra eladhato, de a tortenet megmarad.
     */
    @Transactional
    public void lemondas(long jegyId, long ugyfelId) {
        try {
            Jegy jegy = this.jegyRepository.findById(jegyId)
                    .orElseThrow(() -> new UzletiSzabalyException(Tipus.NEM_TALALHATO, "jegy.nincs", "Nem létező jegy."));
            if (jegy.getUgyfelId() != ugyfelId) {
                throw new UzletiSzabalyException(Tipus.TILTOTT, "jegy.nem.sajat", "Ez nem a te jegyed.");
            }
            if (jegy.getStatus() == JegyStatus.TOROLT) {
                throw new UzletiSzabalyException(Tipus.ERVENYTELEN, "jegy.mar.torolt", "Ez a jegy már le van mondva.");
            }
            Vetites vetites = this.vetitesRepository.findById(jegy.getVetitesId()).orElseThrow();
            if (!vetites.getKezdesIdo().isAfter(LocalDateTime.now())) {
                throw new UzletiSzabalyException(Tipus.ERVENYTELEN, "jegy.lemondas.elmult", "Elkezdődött vetítésre szóló jegy nem mondható le.");
            }

            jegy.setStatus(JegyStatus.TOROLT);
            this.jegyRepository.save(jegy);

        } catch (UzletiSzabalyException e) {
            this.hibaNaploService.naploz(e.getKod(), e.getMessage(), LEMONDAS);
            throw e;
        }
    }
}
