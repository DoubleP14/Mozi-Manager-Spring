package hu.mik.pte.prog5.mozi.security;

import hu.mik.pte.prog5.mozi.repository.UgyfelRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return this.ugyfelRepository.findByEmail(email)
                .map(MoziFelhasznalo::new)
                .orElseThrow(() -> new UsernameNotFoundException(email));
    }
}
