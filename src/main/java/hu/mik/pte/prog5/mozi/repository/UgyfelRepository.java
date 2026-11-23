package hu.mik.pte.prog5.mozi.repository;

import hu.mik.pte.prog5.mozi.model.Ugyfel;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

public interface UgyfelRepository extends ListCrudRepository<Ugyfel, Long> {

    // A bejelentkezes e-mail cim alapjan tortenik
    Optional<Ugyfel> findByEmail(String email);

    boolean existsByEmail(String email);
}
