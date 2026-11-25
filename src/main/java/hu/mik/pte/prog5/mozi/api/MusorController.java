package hu.mik.pte.prog5.mozi.api;

import hu.mik.pte.prog5.mozi.api.dto.VetitesReszletek;
import hu.mik.pte.prog5.mozi.model.nezet.MusorElem;
import hu.mik.pte.prog5.mozi.repository.LekerdezesRepository;
import hu.mik.pte.prog5.mozi.security.MoziFelhasznalo;
import hu.mik.pte.prog5.mozi.service.JegyService;
import hu.mik.pte.prog5.mozi.service.UgyfelService;
import hu.mik.pte.prog5.mozi.service.UzletiSzabalyException;
import hu.mik.pte.prog5.mozi.service.UzletiSzabalyException.Tipus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * A musor belepes nelkul is bongeszheto (a SecurityConfig-ban permitAll).
 */
@RestController
@RequestMapping("/api/musor")
public class MusorController {

    private final LekerdezesRepository lekerdezesRepository;
    private final JegyService jegyService;
    private final UgyfelService ugyfelService;

    public MusorController(LekerdezesRepository lekerdezesRepository, JegyService jegyService,
                           UgyfelService ugyfelService) {
        this.lekerdezesRepository = lekerdezesRepository;
        this.jegyService = jegyService;
        this.ugyfelService = ugyfelService;
    }

    // A jovobeli vetitesek a v_musor nezetbol, idorendben
    @GetMapping
    public List<MusorElem> musor() {
        return this.lekerdezesRepository.findJovobeliMusor();
    }

    // Bejelentkezes nelkul a felhasznalo null, ilyenkor az alapar latszik
    @GetMapping("/{vetitesId}")
    public VetitesReszletek vetites(@PathVariable long vetitesId,
                                    @AuthenticationPrincipal MoziFelhasznalo felhasznalo) {
        MusorElem vetites = this.lekerdezesRepository.findMusorElem(vetitesId)
                .orElseThrow(() -> new UzletiSzabalyException(Tipus.NEM_TALALHATO, "jegy.vetites.nincs",
                        "Nem létező vetítés."));
        int pontok = felhasznalo == null ? 0 : this.ugyfelService.findById(felhasznalo.getUgyfelId())
                .map(u -> u.getTorzsvasarloiPontok())
                .orElse(0);
        return new VetitesReszletek(vetites, this.jegyService.foglaltSzekek(vetitesId), JegyService.egysegar(pontok));
    }
}
