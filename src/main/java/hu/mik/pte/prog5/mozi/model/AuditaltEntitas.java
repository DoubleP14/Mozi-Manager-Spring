package hu.mik.pte.prog5.mozi.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.ReadOnlyProperty;

import java.time.LocalDateTime;

/**
 * Az audit mezok, amik minden tablaban megvannak.
 * Az idopontokat az adatbazis audit_mezok() triggere tolti, ezert azokat nem irjuk (ReadOnlyProperty).
 * A felhasznalot a Spring Data auditing tolti ki az AuditorAware alapjan.
 */
@Getter
@Setter
public abstract class AuditaltEntitas {

    @ReadOnlyProperty
    private LocalDateTime letrehozva;

    @CreatedBy
    private String letrehozta;

    @ReadOnlyProperty
    private LocalDateTime modositva;

    @LastModifiedBy
    private String modositotta;
}
