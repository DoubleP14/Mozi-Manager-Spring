package hu.mik.pte.prog5.mozi.security;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import hu.mik.pte.prog5.mozi.ui.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Ket kulon szuro lanc lesz:
 *  1. /api/**  a Svelte feluletnek: JSON belepes, session cookie, CSRF cookie, atiranyitas helyett 401
 *  2. minden mas: a Vaadin admin felulet (/admin/**), sajat belepo oldallal
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // A sajat belepes vegpont ezzel ellenorzi az e-mail cimet es a jelszot
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    // A bejelentkezes a sessionben tarolodik, a bongeszo a JSESSIONID cookie-val azonositja magat
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                                      SecurityContextRepository securityContextRepository) {
        http.securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/bejelentkezes", "/api/auth/regisztracio").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                // A CSRF token egy XSRF-TOKEN cookie-ba kerul, a Svelte ezt X-XSRF-TOKEN fejlecben kuldi vissza
                .csrf(csrf -> csrf.spa())
                // Bejelentkezes nelkul 401, nem atiranyitas egy login oldalra
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/kijelentkezes")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }

    // Minden mas a Vaadin admin felulete (/admin/**): ugyanaz, mint a ZH-kban.
    // A nezetek jogosultsagat a @RolesAllowed / @PermitAll / @AnonymousAllowed annotaciok adjak.
    @Bean
    @Order(2)
    public SecurityFilterChain vaadinSecurityFilterChain(HttpSecurity http) {
        // A defaultSuccessUrl nelkul belepes utan a / cimre vinne, ami mar nem a Vaadin felulet
        http.with(VaadinSecurityConfigurer.vaadin(), vaadin -> vaadin
                .loginView(LoginView.class)
                .defaultSuccessUrl("/admin/"));
        return http.build();
    }
}
