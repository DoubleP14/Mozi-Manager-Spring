package hu.mik.pte.prog5.mozi.repository;

import hu.mik.pte.prog5.mozi.model.nezet.BevetelSor;
import hu.mik.pte.prog5.mozi.model.nezet.HibaNaploSor;
import hu.mik.pte.prog5.mozi.model.nezet.MusorElem;
import hu.mik.pte.prog5.mozi.model.nezet.SajatJegy;

import java.util.List;
import java.util.Optional;

/**
 * Csak olvaso lekerdezesek a nezetekbol es tobb tabla osszekapcsolasabol.
 * Ezek nem egy entitast toltenek be, ezert JdbcTemplate-tel keszulnek (JdbcLekerdezesRepository).
 */
public interface LekerdezesRepository {

    List<MusorElem> findJovobeliMusor();

    // Az admin feluletnek: minden vetites, a legujabb elol
    List<MusorElem> findTeljesMusor();

    List<HibaNaploSor> findLegutobbiHibak(int darab);

    Optional<MusorElem> findMusorElem(long vetitesId);

    List<BevetelSor> findBevetelek();

    List<SajatJegy> findSajatJegyek(long ugyfelId);

    // Az adatbazis szabad_helyek() fuggvenyet hivja
    int szabadHelyek(long vetitesId);
}
