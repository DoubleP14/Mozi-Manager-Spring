-- Mozi Manager - nezetek

-- MUSOR: az eredeti v_musor, kiegeszitve az azonositokkal es a szabad helyekkel,
-- hogy az alkalmazas kozvetlenul hasznalhassa.
create or replace view v_musor as
select v.vetites_id,
       f.film_id,
       f.cim                       as film_cim,
       k.megnevezes                as kategoria,
       t.terem_id,
       t.nev                       as terem,
       t.kapacitas,
       v.kezdes_ido,
       f.hossz_perc,
       szabad_helyek(v.vetites_id) as szabad_helyek
from vetites v
         join film f on f.film_id = v.film_id
         join terem t on t.terem_id = v.terem_id
         join kategoria k on k.kategoria_id = f.kategoria_id;

-- BEVETEL FILMENKENT: az eredeti v_bevetel_filmenkent.
-- NVL helyett coalesce, es a film_id is benne van.
create or replace view v_bevetel_filmenkent as
select f.film_id,
       f.cim,
       count(j.jegy_id)          as eladott_jegyek,
       coalesce(sum(j.ar), 0)    as osszes_bevetel
from film f
         left join vetites v on v.film_id = f.film_id
         left join jegy j on j.vetites_id = v.vetites_id and j.status = 'FIZETVE'
group by f.film_id, f.cim;
