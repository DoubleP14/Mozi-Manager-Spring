package hu.mik.pte.prog5.mozi.repository;

import hu.mik.pte.prog5.mozi.model.Vetites;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VetitesRepository extends ListCrudRepository<Vetites, Long> {

    List<Vetites> findByFilmId(Long filmId);

    // Zarolja a vetites sorat a tranzakcio vegeig, mint az eredeti jegy_vasarlas eljaras.
    // Igy ugyanarra a vetitesre egyszerre csak egy vasarlas futhat.
    @Query("select * from vetites where vetites_id = :vetitesId for update")
    Optional<Vetites> findByIdForUpdate(@Param("vetitesId") Long vetitesId);

    /**
     * Van-e a teremben olyan masik vetites, ami idoben atfedi a megadott idoszakot.
     * Egy vetites a kezdestol a film hossza + takaritasi ido vegeig foglalja a termet.
     * Ket idoszak akkor fedi egymast, ha mindketto hamarabb kezdodik, mint ahogy a masik veget er.
     */
    @Query("""
            select count(*) > 0
            from vetites v
                     join film f on f.film_id = v.film_id
            where v.terem_id = :teremId
              and v.vetites_id <> coalesce(cast(:vetitesId as bigint), -1)
              and v.kezdes_ido < :veg
              and v.kezdes_ido + (f.hossz_perc + :takaritasPerc) * interval '1 minute' > :kezdes""")
    boolean vanAtfedoVetites(@Param("teremId") Long teremId,
                             @Param("vetitesId") Long vetitesId,
                             @Param("kezdes") LocalDateTime kezdes,
                             @Param("veg") LocalDateTime veg,
                             @Param("takaritasPerc") int takaritasPerc);
}
