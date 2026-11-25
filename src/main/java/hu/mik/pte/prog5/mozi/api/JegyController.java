package hu.mik.pte.prog5.mozi.api;

import hu.mik.pte.prog5.mozi.api.dto.JegyVasarlasKeres;
import hu.mik.pte.prog5.mozi.api.dto.VasarlasEredmeny;
import hu.mik.pte.prog5.mozi.model.Jegy;
import hu.mik.pte.prog5.mozi.model.nezet.SajatJegy;
import hu.mik.pte.prog5.mozi.security.MoziFelhasznalo;
import hu.mik.pte.prog5.mozi.service.JegyService;
import hu.mik.pte.prog5.mozi.service.UgyfelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * A bejelentkezett ugyfel jegyei. Az ugyfel azonositoja mindig a sessionbol jon,
 * soha nem a kerest kuldotol, igy mas neveben nem lehet vasarolni vagy lemondani.
 */
@RestController
@RequestMapping("/api/jegyek")
public class JegyController {

    private final JegyService jegyService;
    private final UgyfelService ugyfelService;

    public JegyController(JegyService jegyService, UgyfelService ugyfelService) {
        this.jegyService = jegyService;
        this.ugyfelService = ugyfelService;
    }

    @GetMapping
    public List<SajatJegy> sajatJegyek(@AuthenticationPrincipal MoziFelhasznalo felhasznalo) {
        return this.jegyService.sajatJegyek(felhasznalo.getUgyfelId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VasarlasEredmeny vasarlas(@Valid @RequestBody JegyVasarlasKeres keres,
                                     @AuthenticationPrincipal MoziFelhasznalo felhasznalo) {
        List<Jegy> jegyek = this.jegyService.vasarlas(keres.vetitesId(), felhasznalo.getUgyfelId(), keres.szekek());
        int egysegar = jegyek.getFirst().getAr();
        int pontok = this.ugyfelService.findById(felhasznalo.getUgyfelId())
                .map(u -> u.getTorzsvasarloiPontok())
                .orElse(0);
        return new VasarlasEredmeny(
                jegyek.stream().map(Jegy::getJegyId).toList(),
                jegyek.stream().map(Jegy::getSzekSzam).toList(),
                egysegar,
                egysegar * jegyek.size(),
                pontok);
    }

    @PostMapping("/{jegyId}/lemondas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void lemondas(@PathVariable long jegyId, @AuthenticationPrincipal MoziFelhasznalo felhasznalo) {
        this.jegyService.lemondas(jegyId, felhasznalo.getUgyfelId());
    }
}
