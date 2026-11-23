package hu.mik.pte.prog5.mozi.api;

import hu.mik.pte.prog5.mozi.api.dto.BejelentkezesKeres;
import hu.mik.pte.prog5.mozi.api.dto.FelhasznaloAdat;
import hu.mik.pte.prog5.mozi.api.dto.RegisztracioKeres;
import hu.mik.pte.prog5.mozi.model.Ugyfel;
import hu.mik.pte.prog5.mozi.security.MoziFelhasznalo;
import hu.mik.pte.prog5.mozi.service.UgyfelService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

/**
 * Belepes, regisztracio es a bejelentkezett felhasznalo lekerdezese a Svelte feluletnek.
 * A kijelentkezest (POST /api/auth/kijelentkezes) a Spring Security logout szuroje kezeli.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UgyfelService ugyfelService;

    public AuthController(AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          UgyfelService ugyfelService) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.ugyfelService = ugyfelService;
    }

    // A felulet induláskor ezt hivja: a valaszban megkapja a CSRF tokent, es beallitodik az XSRF-TOKEN cookie
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of("headerName", csrfToken.getHeaderName(), "token", csrfToken.getToken());
    }

    // Hibas jelszonal a BadCredentialsException-t az ApiHibaKezelo alakitja 401-re
    @PostMapping("/bejelentkezes")
    public FelhasznaloAdat bejelentkezes(@Valid @RequestBody BejelentkezesKeres keres,
                                         HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = this.authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(normalizal(keres.email()), keres.jelszo()));
        return this.beleptet(authentication, request, response);
    }

    // Sikeres regisztracio utan a felhasznalo rogton be is van jelentkezve
    @PostMapping("/regisztracio")
    @ResponseStatus(HttpStatus.CREATED)
    public FelhasznaloAdat regisztracio(@Valid @RequestBody RegisztracioKeres keres,
                                        HttpServletRequest request, HttpServletResponse response) {
        Ugyfel ugyfel = this.ugyfelService.regisztracio(keres.nev(), keres.email(), keres.jelszo());
        MoziFelhasznalo felhasznalo = new MoziFelhasznalo(ugyfel);
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                felhasznalo, null, felhasznalo.getAuthorities());
        return this.beleptet(authentication, request, response);
    }

    // A pontok valtozhatnak (trigger), ezert mindig az adatbazisbol olvassuk ujra
    @GetMapping("/en")
    public FelhasznaloAdat en(@AuthenticationPrincipal MoziFelhasznalo felhasznalo) {
        return this.ugyfelService.findById(felhasznalo.getUgyfelId())
                .map(FelhasznaloAdat::of)
                .orElseThrow();
    }

    private FelhasznaloAdat beleptet(Authentication authentication, HttpServletRequest request,
                                     HttpServletResponse response) {
        // Session fixation elleni vedelem: belepeskor uj session azonosito
        request.getSession(true);
        request.changeSessionId();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        // Elmentjuk a sessionbe, igy a kovetkezo kereseknel is be lesz jelentkezve
        this.securityContextRepository.saveContext(context, request, response);

        MoziFelhasznalo felhasznalo = (MoziFelhasznalo) authentication.getPrincipal();
        return this.en(felhasznalo);
    }

    private static String normalizal(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
