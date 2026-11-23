package hu.mik.pte.prog5.mozi.service;

import hu.mik.pte.prog5.mozi.model.Szerep;
import hu.mik.pte.prog5.mozi.model.Ugyfel;
import hu.mik.pte.prog5.mozi.repository.UgyfelRepository;
import hu.mik.pte.prog5.mozi.service.UzletiSzabalyException.Tipus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class UgyfelService {

    private final UgyfelRepository ugyfelRepository;
    private final PasswordEncoder passwordEncoder;

    public UgyfelService(UgyfelRepository ugyfelRepository, PasswordEncoder passwordEncoder) {
        this.ugyfelRepository = ugyfelRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<Ugyfel> findById(long ugyfelId) {
        return this.ugyfelRepository.findById(ugyfelId);
    }

    /**
     * Uj ugyfel regisztracioja. A szerep mindig UGYFEL, admint csak az adatbazisban lehet letrehozni.
     * A jelszo BCrypt hash-kent tarolodik, sima szovegkent soha.
     */
    @Transactional
    public Ugyfel regisztracio(String nev, String email, String jelszo) {
        String normalizaltEmail = email.trim().toLowerCase(Locale.ROOT);
        if (this.ugyfelRepository.existsByEmail(normalizaltEmail)) {
            throw new UzletiSzabalyException(Tipus.UTKOZES, "ugyfel.email.foglalt",
                    "Ezzel az e-mail címmel már regisztráltak.");
        }
        try {
            Ugyfel ugyfel = new Ugyfel(nev.trim(), normalizaltEmail, this.passwordEncoder.encode(jelszo), Szerep.UGYFEL);
            return this.ugyfelRepository.save(ugyfel);
        } catch (DuplicateKeyException e) {
            // ket egyideju regisztracio ugyanazzal a cimmel: az adatbazis unique megszoritasa dont
            throw new UzletiSzabalyException(Tipus.UTKOZES, "ugyfel.email.foglalt",
                    "Ezzel az e-mail címmel már regisztráltak.");
        }
    }
}
