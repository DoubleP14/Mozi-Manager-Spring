package hu.mik.pte.prog5.mozi.repository;

import hu.mik.pte.prog5.mozi.model.Vetites;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VetitesRepository extends ListCrudRepository<Vetites, Long> {

    List<Vetites> findByFilmId(Long filmId);

    // Zarolja a vetites sorat a tranzakcio vegeig, mint az eredeti jegy_vasarlas eljaras.
    // Igy ugyanarra a vetitesre egyszerre csak egy vasarlas futhat.
    @Query("select * from vetites where vetites_id = :vetitesId for update")
    Optional<Vetites> findByIdForUpdate(@Param("vetitesId") Long vetitesId);
}
