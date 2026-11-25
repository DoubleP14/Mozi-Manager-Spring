package hu.mik.pte.prog5.mozi.api;

import hu.mik.pte.prog5.mozi.TestcontainersConfig;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kozos alap a REST API tesztekhez: MockMvc, valodi CSRF token es belepes.
 * Mivel minden API teszt ugyanezt a beallitast hasznalja, a Spring egy kozos kontextust
 * (es egy kozos adatbazis kontenert) hasznal hozzajuk.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
abstract class ApiTestAlap {

    @Autowired
    protected MockMvc mockMvc;

    /**
     * Valodi CSRF token, ugy, ahogy a Svelte felulet is kapja: GET /api/auth/csrf,
     * majd a cookie erteket az X-XSRF-TOKEN fejlecben kuldjuk vissza.
     * (A spring-security-test csrf() segedfuggvenye a kozos kontextusban lecserelne a cookie-s tarolot.)
     */
    protected RequestPostProcessor csrfToken() throws Exception {
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

    protected static String belepesJson(String email, String jelszo) {
        return "{\"email\":\"" + email + "\",\"jelszo\":\"" + jelszo + "\"}";
    }

    protected MockHttpSession belep(String email, String jelszo) throws Exception {
        MvcResult eredmeny = this.mockMvc.perform(post("/api/auth/bejelentkezes").with(this.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(belepesJson(email, jelszo)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) eredmeny.getRequest().getSession(false);
    }
}
