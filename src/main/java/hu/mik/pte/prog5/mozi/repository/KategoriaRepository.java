package hu.mik.pte.prog5.mozi.repository;

import hu.mik.pte.prog5.mozi.model.Kategoria;
import org.springframework.data.repository.ListCrudRepository;

// A CRUD muveleteket (findAll, findById, save, deleteById...) a Spring Data JDBC adja
public interface KategoriaRepository extends ListCrudRepository<Kategoria, Long> {

    boolean existsByMegnevezes(String megnevezes);
}
