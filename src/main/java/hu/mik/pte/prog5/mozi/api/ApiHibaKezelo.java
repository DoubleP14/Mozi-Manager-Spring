package hu.mik.pte.prog5.mozi.api;

import hu.mik.pte.prog5.mozi.api.dto.HibaValasz;
import hu.mik.pte.prog5.mozi.service.UzletiSzabalyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A REST API kivetelei egyseges JSON valaszkent: { kod, uzenet, mezok }.
 * Igy a felulet a kod alapjan tud forditott hibauzenetet mutatni.
 */
@RestControllerAdvice(basePackages = "hu.mik.pte.prog5.mozi.api")
public class ApiHibaKezelo {

    @ExceptionHandler(UzletiSzabalyException.class)
    public ResponseEntity<HibaValasz> uzletiSzabaly(UzletiSzabalyException e) {
        HttpStatus status = switch (e.getTipus()) {
            case ERVENYTELEN -> HttpStatus.BAD_REQUEST;
            case NEM_TALALHATO -> HttpStatus.NOT_FOUND;
            case UTKOZES -> HttpStatus.CONFLICT;
            case TILTOTT -> HttpStatus.FORBIDDEN;
        };
        return ResponseEntity.status(status).body(new HibaValasz(e.getKod(), e.getMessage()));
    }

    // @Valid hiba: mezonkent a hibauzenet
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<HibaValasz> validacio(MethodArgumentNotValidException e) {
        Map<String, String> mezok = new LinkedHashMap<>();
        for (FieldError hiba : e.getBindingResult().getFieldErrors()) {
            mezok.putIfAbsent(hiba.getField(), hiba.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(new HibaValasz("validacio", "Hibás adatok.", mezok));
    }

    // Hibas e-mail cim vagy jelszo. Szandekosan nem arulja el, melyik volt rossz.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<HibaValasz> hibasBelepes(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new HibaValasz("auth.hibas", "Hibás e-mail cím vagy jelszó."));
    }
}
