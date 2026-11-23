package hu.mik.pte.prog5.mozi.service;

import hu.mik.pte.prog5.mozi.TestcontainersConfig;
import hu.mik.pte.prog5.mozi.model.Jegy;
import hu.mik.pte.prog5.mozi.model.JegyStatus;
import hu.mik.pte.prog5.mozi.model.Ugyfel;
import hu.mik.pte.prog5.mozi.model.nezet.MusorElem;
import hu.mik.pte.prog5.mozi.repository.HibaNaploRepository;
import hu.mik.pte.prog5.mozi.repository.JegyRepository;
import hu.mik.pte.prog5.mozi.repository.LekerdezesRepository;
import hu.mik.pte.prog5.mozi.repository.UgyfelRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Docker nelkul (pl. a tanar gepen) ezek a tesztek kimaradnak, nem buknak el
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@Import(TestcontainersConfig.class)
class JegyServiceTest {

    @Autowired
    private JegyService jegyService;
    @Autowired
    private LekerdezesRepository lekerdezesRepository;
    @Autowired
    private UgyfelRepository ugyfelRepository;
    @Autowired
    private JegyRepository jegyRepository;
    @Autowired
    private HibaNaploRepository hibaNaploRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // A tesztek ugyanazt az adatbazist hasznaljak, ezert mindegyik mas vetitest vagy szeket hasznal
    private MusorElem vetites(String filmCim) {
        return this.lekerdezesRepository.findJovobeliMusor().stream()
                .filter(m -> m.filmCim().equals(filmCim))
                .max(Comparator.comparing(MusorElem::kezdesIdo))
                .orElseThrow();
    }

    private Ugyfel ugyfel(String email) {
        return this.ugyfelRepository.findByEmail(email).orElseThrow();
    }

    @Test
    void vasarlasAlapArPontokkalEsSzabadHellyel() {
        MusorElem vetites = vetites("Oppenheimer");
        Ugyfel janos = ugyfel("janos@example.com");
        int szabadElotte = this.lekerdezesRepository.szabadHelyek(vetites.vetitesId());

        Jegy jegy = this.jegyService.vasarlas(vetites.vetitesId(), janos.getUgyfelId(), 1);

        assertThat(jegy.getJegyId()).isNotNull();
        assertThat(jegy.getAr()).isEqualTo(JegyService.ALAP_AR);
        assertThat(ugyfel("janos@example.com").getTorzsvasarloiPontok())
                .isEqualTo(janos.getTorzsvasarloiPontok() + 10);
        assertThat(this.lekerdezesRepository.szabadHelyek(vetites.vetitesId())).isEqualTo(szabadElotte - 1);
        assertThat(this.jegyService.foglaltSzekek(vetites.vetitesId())).contains(1);
    }

    @Test
    void otvenPontFelettKedvezmenyesAr() {
        MusorElem vetites = vetites("Oppenheimer");
        Ugyfel elek = ugyfel("elek@example.com");
        assertThat(elek.getTorzsvasarloiPontok()).isGreaterThan(JegyService.KEDVEZMENY_PONTHATAR);

        Jegy jegy = this.jegyService.vasarlas(vetites.vetitesId(), elek.getUgyfelId(), 2);

        assertThat(jegy.getAr()).isEqualTo(JegyService.KEDVEZMENYES_AR);
    }

    @Test
    void foglaltSzekreHibaEsANaploBejegyzesMegmarad() {
        MusorElem vetites = vetites("Agymanók 2");
        long ugyfelId = ugyfel("janos@example.com").getUgyfelId();
        this.jegyService.vasarlas(vetites.vetitesId(), ugyfelId, 7);
        long naploElotte = this.hibaNaploRepository.count();
        long jegyekElotte = this.jegyRepository.count();

        assertThatThrownBy(() -> this.jegyService.vasarlas(vetites.vetitesId(), ugyfelId, 7))
                .isInstanceOf(UzletiSzabalyException.class)
                .extracting("kod").isEqualTo("jegy.szek.foglalt");

        // A vasarlas visszagordult, de a REQUIRES_NEW miatt a hibanaplo bejegyzes megmaradt
        assertThat(this.jegyRepository.count()).isEqualTo(jegyekElotte);
        assertThat(this.hibaNaploRepository.count()).isEqualTo(naploElotte + 1);
    }

    @Test
    void nemLetezoSzekreHiba() {
        MusorElem vetites = vetites("Agymanók 2");
        long ugyfelId = ugyfel("janos@example.com").getUgyfelId();

        assertThatThrownBy(() -> this.jegyService.vasarlas(vetites.vetitesId(), ugyfelId, vetites.kapacitas() + 1))
                .isInstanceOf(UzletiSzabalyException.class)
                .extracting("kod").isEqualTo("jegy.szek.ervenytelen");
    }

    @Test
    void elmultVetitesreNemLehetJegyetVenni() {
        Long elmult = this.jdbcTemplate.queryForObject(
                "select vetites_id from vetites where kezdes_ido < now() order by kezdes_ido limit 1", Long.class);
        long ugyfelId = ugyfel("janos@example.com").getUgyfelId();

        assertThatThrownBy(() -> this.jegyService.vasarlas(elmult, ugyfelId, 99))
                .isInstanceOf(UzletiSzabalyException.class)
                .extracting("kod").isEqualTo("jegy.vetites.elmult");
    }

    @Test
    void lemondasUtanASzekUjraEladhato() {
        MusorElem vetites = vetites("Avatar: A víz útja");
        long janos = ugyfel("janos@example.com").getUgyfelId();
        long elek = ugyfel("elek@example.com").getUgyfelId();
        Jegy jegy = this.jegyService.vasarlas(vetites.vetitesId(), janos, 15);

        // mas nem mondhatja le
        assertThatThrownBy(() -> this.jegyService.lemondas(jegy.getJegyId(), elek))
                .isInstanceOf(UzletiSzabalyException.class)
                .extracting("kod").isEqualTo("jegy.nem.sajat");

        this.jegyService.lemondas(jegy.getJegyId(), janos);

        assertThat(this.jegyRepository.findById(jegy.getJegyId()).orElseThrow().getStatus())
                .isEqualTo(JegyStatus.TOROLT);
        assertThat(this.jegyService.foglaltSzekek(vetites.vetitesId())).doesNotContain(15);
        assertThat(this.jegyService.vasarlas(vetites.vetitesId(), elek, 15).getJegyId()).isNotNull();
        assertThat(this.jegyService.sajatJegyek(janos))
                .anyMatch(j -> j.jegyId().equals(jegy.getJegyId()) && j.status() == JegyStatus.TOROLT);
    }

    // Az AuditorAware a bejelentkezett felhasznalot irja a letrehozta mezobe, a trigger nem irja felul
    @Test
    @WithMockUser(username = "elek@example.com")
    void aJegyLetrehozojaABejelentkezettFelhasznalo() {
        MusorElem vetites = vetites("Barbie");
        long elek = ugyfel("elek@example.com").getUgyfelId();

        Jegy jegy = this.jegyService.vasarlas(vetites.vetitesId(), elek, 20);

        String letrehozta = this.jdbcTemplate.queryForObject(
                "select letrehozta from jegy where jegy_id = ?", String.class, jegy.getJegyId());
        String naplozo = this.jdbcTemplate.queryForObject(
                "select modosito_felhasznalo from jegy_history where jegy_id = ? and muvelet = 'INSERT'",
                String.class, jegy.getJegyId());
        assertThat(letrehozta).isEqualTo("elek@example.com");
        assertThat(naplozo).isEqualTo("elek@example.com");
    }

    @Test
    void bevetelekANezetbol() {
        assertThat(this.lekerdezesRepository.findBevetelek())
                .anyMatch(b -> b.cim().equals("Dűne: Második rész") && b.osszesBevetel() >= 4500);
    }

    @Test
    void nemLetezoVetitesreNullaSzabadHely() {
        assertThat(this.lekerdezesRepository.szabadHelyek(-1)).isZero();
    }
}
