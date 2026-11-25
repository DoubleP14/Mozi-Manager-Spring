package hu.mik.pte.prog5.mozi.security;

import hu.mik.pte.prog5.mozi.repository.UgyfelRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Belepeskor a Spring Security ezen keresztul keresi meg a felhasznalot az adatbazisban.
 * A jelszo ellenorzeset mar a Spring Security vegzi a PasswordEncoder-rel.
 */
@Service
public class MoziUserDetailsService implements UserDetailsService {

    private final UgyfelRepository ugyfelRepository;

    public MoziUserDetailsService(UgyfelRepository ugyfelRepository) {
        this.ugyfelRepository = ugyfelRepository;
    }

    // A REST API es a Vaadin belepes is ezt hivja, ezert itt egysegesitjuk az e-mail cimet
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizalt = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return this.ugyfelRepository.findByEmail(normalizalt)
                .map(MoziFelhasznalo::new)
                .orElseThrow(() -> new UsernameNotFoundException(email));
    }
}
