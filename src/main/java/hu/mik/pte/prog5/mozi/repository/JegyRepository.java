package hu.mik.pte.prog5.mozi.repository;

import hu.mik.pte.prog5.mozi.model.Jegy;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JegyRepository extends ListCrudRepository<Jegy, Long> {

    // Sajat SQL: a vetites foglalt (nem torolt) szekei, a szekvalaszto ezeket tiltja le
    @Query("select szek_szam from jegy where vetites_id = :vetitesId and status <> 'TOROLT' order by szek_szam")
    List<Integer> findFoglaltSzekek(@Param("vetitesId") Long vetitesId);

    @Query("select count(*) > 0 from jegy where vetites_id = :vetitesId and szek_szam = :szekSzam and status <> 'TOROLT'")
    boolean isSzekFoglalt(@Param("vetitesId") Long vetitesId, @Param("szekSzam") Integer szekSzam);

    @Query("select count(*) from jegy where vetites_id = :vetitesId and status <> 'TOROLT'")
    int countAktivJegyek(@Param("vetitesId") Long vetitesId);

    // A terem jovobeli vetitesein eladott legnagyobb szekszam: a kapacitas ez ala nem csokkenhet
    @Query("""
            select coalesce(max(j.szek_szam), 0)
            from jegy j
                     join vetites v on v.vetites_id = j.vetites_id
            where v.terem_id = :teremId
              and j.status <> 'TOROLT'
              and v.kezdes_ido > now()""")
    int maxEladottSzek(@Param("teremId") Long teremId);
}
