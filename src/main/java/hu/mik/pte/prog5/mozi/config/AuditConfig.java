package hu.mik.pte.prog5.mozi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

import java.util.Optional;

// Bekapcsolja a @CreatedBy / @LastModifiedBy mezok kitolteset
@Configuration
@EnableJdbcAuditing
public class AuditConfig {

    // Ki vegzi a muveletet. Ha ures, az adatbazis trigger a current_user erteket irja be.
    // TODO 3. lepes: a bejelentkezett felhasznalo e-mail cime a SecurityContextbol
    @Bean
    public AuditorAware<String> auditorAware() {
        return Optional::empty;
    }
}
