package hu.mik.pte.prog5.mozi.repository;

import hu.mik.pte.prog5.mozi.model.JegyStatus;
import hu.mik.pte.prog5.mozi.model.nezet.BevetelSor;
import hu.mik.pte.prog5.mozi.model.nezet.MusorElem;
import hu.mik.pte.prog5.mozi.model.nezet.SajatJegy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcLekerdezesRepository implements LekerdezesRepository {

    private static final String MUSOR_OSZLOPOK = """
            select vetites_id, film_id, film_cim, kategoria, terem_id, terem, kapacitas,
                   kezdes_ido, hossz_perc, szabad_helyek
            from v_musor""";

    // Egy sor -> egy record. A RowMapper-t a lekerdezesek ujrahasznaljak.
    private static final RowMapper<MusorElem> MUSOR_MAPPER = (rs, rowNum) -> new MusorElem(
            rs.getLong("vetites_id"),
            rs.getLong("film_id"),
            rs.getString("film_cim"),
            rs.getString("kategoria"),
            rs.getLong("terem_id"),
            rs.getString("terem"),
            rs.getInt("kapacitas"),
            rs.getTimestamp("kezdes_ido").toLocalDateTime(),
            rs.getInt("hossz_perc"),
            rs.getInt("szabad_helyek"));

    private static final RowMapper<BevetelSor> BEVETEL_MAPPER = (rs, rowNum) -> new BevetelSor(
            rs.getLong("film_id"),
            rs.getString("cim"),
            rs.getLong("eladott_jegyek"),
            rs.getLong("osszes_bevetel"));

    private static final RowMapper<SajatJegy> SAJAT_JEGY_MAPPER = (rs, rowNum) -> new SajatJegy(
            rs.getLong("jegy_id"),
            rs.getLong("vetites_id"),
            rs.getString("film_cim"),
            rs.getString("terem"),
            rs.getTimestamp("kezdes_ido").toLocalDateTime(),
            rs.getInt("szek_szam"),
            rs.getInt("ar"),
            JegyStatus.valueOf(rs.getString("status")));

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final SimpleJdbcCall szabadHelyekCall;

    public JdbcLekerdezesRepository(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.szabadHelyekCall = new SimpleJdbcCall(jdbcTemplate).withFunctionName("szabad_helyek");
    }

    @Override
    public List<MusorElem> findJovobeliMusor() {
        return this.jdbcTemplate.query(MUSOR_OSZLOPOK + " where kezdes_ido > now() order by kezdes_ido", MUSOR_MAPPER);
    }

    @Override
    public Optional<MusorElem> findMusorElem(long vetitesId) {
        return this.jdbcTemplate.query(MUSOR_OSZLOPOK + " where vetites_id = ?", MUSOR_MAPPER, vetitesId)
                .stream()
                .findFirst();
    }

    @Override
    public List<BevetelSor> findBevetelek() {
        return this.jdbcTemplate.query("""
                select film_id, cim, eladott_jegyek, osszes_bevetel
                from v_bevetel_filmenkent
                order by osszes_bevetel desc, cim""", BEVETEL_MAPPER);
    }

    @Override
    public List<SajatJegy> findSajatJegyek(long ugyfelId) {
        return this.namedParameterJdbcTemplate.query("""
                        select j.jegy_id, j.vetites_id, f.cim as film_cim, t.nev as terem,
                               v.kezdes_ido, j.szek_szam, j.ar, j.status
                        from jegy j
                                 join vetites v on v.vetites_id = j.vetites_id
                                 join film f on f.film_id = v.film_id
                                 join terem t on t.terem_id = v.terem_id
                        where j.ugyfel_id = :ugyfelId
                        order by v.kezdes_ido desc, j.szek_szam""",
                new MapSqlParameterSource("ugyfelId", ugyfelId),
                SAJAT_JEGY_MAPPER);
    }

    @Override
    public int szabadHelyek(long vetitesId) {
        Integer eredmeny = this.szabadHelyekCall.executeFunction(Integer.class,
                new MapSqlParameterSource("p_vetites_id", vetitesId));
        return eredmeny == null ? 0 : eredmeny;
    }
}
