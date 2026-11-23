package hu.mik.pte.prog5.mozi.repository;

import hu.mik.pte.prog5.mozi.model.Film;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

public interface FilmRepository extends ListCrudRepository<Film, Long> {

    // A metodus nevebol general lekerdezest: ... where kategoria_id = ?
    List<Film> findByKategoriaId(Long kategoriaId);
}
