package hu.mik.pte.prog5.mozi.api;

import hu.mik.pte.prog5.mozi.TestcontainersConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class AuthApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Valodi CSRF token, ugy, ahogy a Svelte felulet is kapja: GET /api/auth/csrf,
     * majd a cookie erteket az X-XSRF-TOKEN fejlecben kuldjuk vissza.
     * (A spring-security-test csrf() segedfuggvenye a kozos kontextusban lecserelne a cookie-s tarolot.)
     */
    private RequestPostProcessor csrfToken() throws Exception {
        String setCookie = this.mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).startsWith("XSRF-TOKEN=");
        String ertek = setCookie.substring("XSRF-TOKEN=".length(), setCookie.indexOf(';'));
        return request -> {
            request.setCookies(new Cookie("XSRF-TOKEN", ertek));
            request.addHeader("X-XSRF-TOKEN", ertek);
            return request;
        };
    }

    private static String belepesJson(String email, String jelszo) {
        return "{\"email\":\"" + email + "\",\"jelszo\":\"" + jelszo + "\"}";
    }

    private MockHttpSession belep(String email, String jelszo) throws Exception {
        MvcResult eredmeny = this.mockMvc.perform(post("/api/auth/bejelentkezes").with(this.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(belepesJson(email, jelszo)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) eredmeny.getRequest().getSession(false);
    }

    @Test
    void bejelentkezesNelkul401() throws Exception {
        this.mockMvc.perform(get("/api/auth/en")).andExpect(status().isUnauthorized());
    }

    @Test
    void sikeresBelepesUtanAzEnVegpontAFelhasznalotAdja() throws Exception {
        MockHttpSession session = belep("janos@example.com", "janos");

        this.mockMvc.perform(get("/api/auth/en").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("janos@example.com"))
                .andExpect(jsonPath("$.szerep").value("UGYFEL"))
                .andExpect(jsonPath("$.nev").value("Minta János"))
                .andExpect(jsonPath("$.jelszoHash").doesNotExist());
    }

    @Test
    void emailCimKisNagybetuFuggetlen() throws Exception {
        belep("  Elek@Example.com ", "elek");
    }

    @Test
    void hibasJelszoraEgysegesHibauzenet() throws Exception {
        this.mockMvc.perform(post("/api/auth/bejelentkezes").with(this.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(belepesJson("janos@example.com", "rossz")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.kod").value("auth.hibas"));

        this.mockMvc.perform(post("/api/auth/bejelentkezes").with(this.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(belepesJson("nincs@ilyen.hu", "barmi")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.kod").value("auth.hibas"));
    }

    @Test
    void csrfTokenNelkulTiltott() throws Exception {
        this.mockMvc.perform(post("/api/auth/bejelentkezes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(belepesJson("janos@example.com", "janos")))
                .andExpect(status().isForbidden());
    }

    // Ugyanaz, mint amit a Svelte felulet csinal: a cookie-bol kapott tokent fejlecben kuldi vissza
    @Test
    void csrfTokenACookiebolEsFejlecben() throws Exception {
        MvcResult csrfValasz = this.mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        // A Spring Security Set-Cookie fejleckent irja ki, igy onnan olvassuk
        String setCookie = csrfValasz.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).startsWith("XSRF-TOKEN=");
        Cookie cookie = new Cookie("XSRF-TOKEN", setCookie.substring("XSRF-TOKEN=".length(), setCookie.indexOf(';')));

        this.mockMvc.perform(post("/api/auth/bejelentkezes")
                        .cookie(cookie)
                        .header("X-XSRF-TOKEN", cookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(belepesJson("janos@example.com", "janos")))
                .andExpect(status().isOk());
    }

    @Test
    void regisztracioUtanBeVanJelentkezveEsBCryptHashTarolodik() throws Exception {
        MvcResult eredmeny = this.mockMvc.perform(post("/api/auth/regisztracio").with(this.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nev\":\"Új Ügyfél\",\"email\":\"Uj@Example.com\",\"jelszo\":\"titok123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("uj@example.com"))
                .andExpect(jsonPath("$.szerep").value("UGYFEL"))
                .andExpect(jsonPath("$.torzsvasarloiPontok").value(0))
                .andReturn();
        MockHttpSession session = (MockHttpSession) eredmeny.getRequest().getSession(false);

        this.mockMvc.perform(get("/api/auth/en").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nev").value("Új Ügyfél"));

        String hash = this.jdbcTemplate.queryForObject(
                "select jelszo_hash from ugyfel where email = 'uj@example.com'", String.class);
        assertThat(hash).startsWith("$2a$").doesNotContain("titok123");

        // es az uj jelszoval be is tud lepni
        belep("uj@example.com", "titok123");
    }

    @Test
    void foglaltEmailCimmelNemLehetRegisztralni() throws Exception {
        this.mockMvc.perform(post("/api/auth/regisztracio").with(this.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nev\":\"Másik János\",\"email\":\"janos@example.com\",\"jelszo\":\"titok123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.kod").value("ugyfel.email.foglalt"));
    }

    @Test
    void hibasRegisztraciosAdatokraMezonkentiHiba() throws Exception {
        this.mockMvc.perform(post("/api/auth/regisztracio").with(this.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nev\":\"\",\"email\":\"nem-email\",\"jelszo\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.kod").value("validacio"))
                .andExpect(jsonPath("$.mezok.nev").exists())
                .andExpect(jsonPath("$.mezok.email").exists())
                .andExpect(jsonPath("$.mezok.jelszo").exists());
    }

    @Test
    void kijelentkezesUtanNincsBejelentkezve() throws Exception {
        MockHttpSession session = belep("janos@example.com", "janos");

        this.mockMvc.perform(post("/api/auth/kijelentkezes").with(this.csrfToken()).session(session))
                .andExpect(status().isNoContent());
        this.mockMvc.perform(get("/api/auth/en").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ugyfelNemEriElAzAdminVegpontokat() throws Exception {
        MockHttpSession session = belep("janos@example.com", "janos");

        this.mockMvc.perform(get("/api/admin/barmi").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void azApinKivuliCimekTiltottak() throws Exception {
        this.mockMvc.perform(get("/valami")).andExpect(status().isForbidden());
    }
}
