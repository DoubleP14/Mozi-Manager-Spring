package hu.mik.pte.prog5.mozi.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JegyApiTest extends ApiTestAlap {

    // Minden teszt sajat, tavoli vetitest kap, igy a szekek nem utkoznek
    private static final AtomicInteger NAP = new AtomicInteger(30);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long ujVetites() {
        return this.jdbcTemplate.queryForObject("""
                        insert into vetites (film_id, terem_id, kezdes_ido)
                        values ((select film_id from film where cim = 'Barbie'),
                                (select terem_id from terem where nev = 'Nagyterem'),
                                date_trunc('day', now()) + ? * interval '1 day' + interval '18 hours')
                        returning vetites_id""",
                Long.class, NAP.getAndIncrement());
    }

    private static String vasarlasJson(long vetitesId, int... szekek) {
        StringBuilder lista = new StringBuilder();
        for (int i = 0; i < szekek.length; i++) {
            lista.append(i == 0 ? "" : ",").append(szekek[i]);
        }
        return "{\"vetitesId\":" + vetitesId + ",\"szekek\":[" + lista + "]}";
    }

    @Test
    void aMusorBelepesNelkulIsLathato() throws Exception {
        this.mockMvc.perform(get("/api/musor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filmCim").exists())
                .andExpect(jsonPath("$[0].szabadHelyek").isNumber());
    }

    @Test
    void vetitesReszletekAlapEsKedvezmenyesArral() throws Exception {
        long vetites = ujVetites();

        // bejelentkezes nelkul az alapar
        this.mockMvc.perform(get("/api/musor/" + vetites))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vetites.filmCim").value("Barbie"))
                .andExpect(jsonPath("$.vetites.kapacitas").value(120))
                .andExpect(jsonPath("$.egysegar").value(2500));

        // Teszt Elek 50 pont felett van, neki kedvezmenyes
        MockHttpSession elek = belep("elek@example.com", "elek");
        this.mockMvc.perform(get("/api/musor/" + vetites).session(elek))
                .andExpect(jsonPath("$.egysegar").value(2000));
    }

    @Test
    void nemLetezoVetites404() throws Exception {
        this.mockMvc.perform(get("/api/musor/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.kod").value("jegy.vetites.nincs"));
    }

    @Test
    void vasarlasBelepesNelkul401() throws Exception {
        this.mockMvc.perform(post("/api/jegyek").with(this.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vasarlasJson(ujVetites(), 1)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tobbSzekVasarlasaEgyszerreEsAJegyeimListaban() throws Exception {
        long vetites = ujVetites();
        MockHttpSession janos = belep("janos@example.com", "janos");

        this.mockMvc.perform(post("/api/jegyek").with(this.csrfToken()).session(janos)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vasarlasJson(vetites, 1, 2, 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.szekek", hasItems(1, 2, 3)))
                .andExpect(jsonPath("$.egysegar").value(2500))
                .andExpect(jsonPath("$.osszeg").value(7500));

        this.mockMvc.perform(get("/api/musor/" + vetites))
                .andExpect(jsonPath("$.foglaltSzekek", hasItems(1, 2, 3)))
                .andExpect(jsonPath("$.vetites.szabadHelyek").value(117));
        this.mockMvc.perform(get("/api/jegyek").session(janos))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.vetitesId == " + vetites + ")].szekSzam", hasItems(1, 2, 3)));
    }

    // Ha az egyik szek foglalt, a tobbi sem kerul megvasarlasra (egy tranzakcio)
    @Test
    void foglaltSzekeseteMindenVisszagordul() throws Exception {
        long vetites = ujVetites();
        MockHttpSession janos = belep("janos@example.com", "janos");
        this.mockMvc.perform(post("/api/jegyek").with(this.csrfToken()).session(janos)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vasarlasJson(vetites, 5)))
                .andExpect(status().isCreated());

        MockHttpSession elek = belep("elek@example.com", "elek");
        this.mockMvc.perform(post("/api/jegyek").with(this.csrfToken()).session(elek)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vasarlasJson(vetites, 6, 5)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.kod").value("jegy.szek.foglalt"));

        this.mockMvc.perform(get("/api/musor/" + vetites))
                .andExpect(jsonPath("$.foglaltSzekek", not(hasItem(6))));
    }

    @Test
    void ugyanazASzekKetszerNemSzerepelhet() throws Exception {
        MockHttpSession janos = belep("janos@example.com", "janos");

        this.mockMvc.perform(post("/api/jegyek").with(this.csrfToken()).session(janos)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vasarlasJson(ujVetites(), 7, 7)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.kod").value("jegy.szek.tobbszor"));
    }

    @Test
    void ureslistaValidaciosHiba() throws Exception {
        MockHttpSession janos = belep("janos@example.com", "janos");

        this.mockMvc.perform(post("/api/jegyek").with(this.csrfToken()).session(janos)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vasarlasJson(ujVetites())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.kod").value("validacio"));
    }

    @Test
    void lemondasCsakASajatJegyre() throws Exception {
        long vetites = ujVetites();
        MockHttpSession janos = belep("janos@example.com", "janos");
        this.mockMvc.perform(post("/api/jegyek").with(this.csrfToken()).session(janos)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vasarlasJson(vetites, 9)))
                .andExpect(status().isCreated());
        Long jegyId = this.jdbcTemplate.queryForObject(
                "select jegy_id from jegy where vetites_id = ? and szek_szam = 9", Long.class, vetites);

        MockHttpSession elek = belep("elek@example.com", "elek");
        this.mockMvc.perform(post("/api/jegyek/" + jegyId + "/lemondas").with(this.csrfToken()).session(elek))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.kod").value("jegy.nem.sajat"));

        this.mockMvc.perform(post("/api/jegyek/" + jegyId + "/lemondas").with(this.csrfToken()).session(janos))
                .andExpect(status().isNoContent());
        this.mockMvc.perform(get("/api/musor/" + vetites))
                .andExpect(jsonPath("$.foglaltSzekek", not(hasItem(9))));
    }
}
