package hu.mik.pte.prog5.mozi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

// Bekapcsolja a @CreatedBy / @LastModifiedBy mezok kitolteset
@Configuration
@EnableJdbcAuditing
public class AuditConfig {

    // A bejelentkezett felhasznalo e-mail cime kerul a letrehozta / modositotta mezokbe.
    // Bejelentkezes nelkul (pl. regisztracio) ures, ilyenkor az adatbazis trigger a current_user erteket irja be.
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .filter(auth -> !(auth instanceof AnonymousAuthenticationToken))
                .map(Authentication::getName);
    }
}
