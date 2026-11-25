package hu.mik.pte.prog5.mozi.service;

import hu.mik.pte.prog5.mozi.TestcontainersConfig;
import hu.mik.pte.prog5.mozi.model.Film;
import hu.mik.pte.prog5.mozi.model.Kategoria;
import hu.mik.pte.prog5.mozi.model.Terem;
import hu.mik.pte.prog5.mozi.model.Vetites;
import hu.mik.pte.prog5.mozi.repository.UgyfelRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@Import(TestcontainersConfig.class)
@WithMockUser(username = "admin@mozi.hu", roles = "ADMIN")
class AdminServiceTest {

    @Autowired
    private AdminService adminService;
    @Autowired
    private JegyService jegyService;
    @Autowired
    private UgyfelRepository ugyfelRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Terem terem(String nev) {
        return this.adminService.termek().stream().filter(t -> t.getNev().equals(nev)).findFirst().orElseThrow();
    }

    private Film film(String cim) {
        return this.adminService.filmek().stream().filter(f -> f.getCim().equals(cim)).findFirst().orElseThrow();
    }

    // Egy tavoli nap adott oraja, hogy ne utkozzon a kezdo adatok vetiteseivel
    private static LocalDateTime nap(int napMulva, int ora) {
        return LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).plusDays(napMulva).withHour(ora);
    }

    @Test
    @WithMockUser(username = "janos@example.com", roles = "UGYFEL")
    void ugyfelNemHivhatjaAzAdminMuveleteket() {
        assertThatThrownBy(() -> this.adminService.kategoriak()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> this.adminService.kategoriaMentes(new Kategoria("Horror")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void ujKategoriaAuditMezokkel() {
        Kategoria mentett = this.adminService.kategoriaMentes(new Kategoria("Horror"));

        assertThat(mentett.getKategoriaId()).isNotNull();
        String letrehozta = this.jdbcTemplate.queryForObject(
                "select letrehozta from kategoria where kategoria_id = ?", String.class, mentett.getKategoriaId());
        assertThat(letrehozta).isEqualTo("admin@mozi.hu");
    }

    @Test
    void azonosNevuKategoriaUtkozes() {
        assertThatThrownBy(() -> this.adminService.kategoriaMentes(new Kategoria("Dráma")))
                .isInstanceOf(UzletiSzabalyException.class)
                .extracting("kod").isEqualTo("kategoria.letezik");
    }

    @Test
    void hasznaltKategoriaNemTorolheto() {
        Long sciFi = this.adminService.kategoriak().stream()
                .filter(k -> k.getMegnevezes().equals("Sci-Fi")).findFirst().orElseThrow().getKategoriaId();

        assertThatThrownBy(() -> this.adminService.kategoriaTorles(sciFi))
                .isInstanceOf(UzletiSzabalyException.class)
                .extracting("kod").isEqualTo("kategoria.hasznalatban");
        assertThat(this.adminService.kategoriak()).anyMatch(k -> k.getKategoriaId().equals(sciFi));
    }

    @Test
    void atfedoVetitesNemVehetoFel() {
        Terem kisterem = terem("Kisterem");
        Film dune = film("Dűne: Második rész"); // 166 perc
        this.adminService.vetitesMentes(new Vetites(dune.getFilmId(), kisterem.getTeremId(), nap(10, 14)));

        // 14:00 + 166 perc + 15 perc takaritas = 17:01, tehat a 17:00-s kezdes utkozik
        assertThatThrownBy(() -> this.adminService.vetitesMentes(
                new Vetites(dune.getFilmId(), kisterem.getTeremId(), nap(10, 17))))
                .isInstanceOf(UzletiSzabalyException.class)
                .extracting("kod").isEqualTo("vetites.atfedes");

        // masik teremben vagy kesobb mar mehet
        assertThat(this.adminService.vetitesMentes(
                new Vetites(dune.getFilmId(), terem("VIP Terem").getTeremId(), nap(10, 17))).getVetitesId()).isNotNull();
        assertThat(this.adminService.vetitesMentes(
                new Vetites(dune.getFilmId(), kisterem.getTeremId(), nap(10, 18))).getVetitesId()).isNotNull();
    }

    @Test
    void multbeliUjVetitesNemVehetoFel() {
        assertThatThrownBy(() -> this.adminService.vetitesMentes(
                new Vetites(film("Barbie").getFilmId(), terem("Nagyterem").getTeremId(), LocalDateTime.now().minusDays(1))))
                .isInstanceOf(UzletiSzabalyException.class)
                .extracting("kod").isEqualTo("vetites.multban");
    }

    @Test
    void jegyesVetitesTermeNemCserelhetoEsNemTorolheto() {
        Vetites vetites = this.adminService.vetitesMentes(
                new Vetites(film("Barbie").getFilmId(), terem("Nagyterem").getTeremId(), nap(12, 20)));
        long janos = this.ugyfelRepository.findByEmail("janos@example.com").orElseThrow().getUgyfelId();
        this.jegyService.vasarlas(vetites.getVetitesId(), janos, 100);

        vetites.setTeremId(terem("Kisterem").getTeremId());
        assertThatThrownBy(() -> this.adminService.vetitesMentes(vetites))
                .isInstanceOf(UzletiSzabalyException.class)
                .extracting("kod").isEqualTo("vetites.terem.jegyek");

        assertThatThrownBy(() -> this.adminService.vetitesTorles(vetites.getVetitesId()))
                .isInstanceOf(UzletiSzabalyException.class)
                .extracting("kod").isEqualTo("vetites.hasznalatban");

        // a Nagyterem kapacitasa nem csokkenhet a 100. szek ala
        Terem nagyterem = terem("Nagyterem");
        nagyterem.setKapacitas(50);
        assertThatThrownBy(() -> this.adminService.teremMentes(nagyterem))
                .isInstanceOf(UzletiSzabalyException.class)
                .extracting("kod").isEqualTo("terem.kapacitas.kicsi");
    }
}
