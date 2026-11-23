-- Mozi Manager - kezdo adatok
-- A vetitesek idopontja a mai naphoz kepest szamolodik, igy mindig van jovobeli musor.

insert into kategoria (megnevezes)
values ('Akció'),
       ('Vígjáték'),
       ('Sci-Fi'),
       ('Dráma'),
       ('Animáció');

insert into terem (nev, kapacitas)
values ('Nagyterem', 120),
       ('VIP Terem', 30),
       ('Kisterem', 60);

insert into film (cim, hossz_perc, kategoria_id)
values ('Mission Impossible', 140, (select kategoria_id from kategoria where megnevezes = 'Akció')),
       ('Barbie', 114, (select kategoria_id from kategoria where megnevezes = 'Vígjáték')),
       ('Avatar: A víz útja', 192, (select kategoria_id from kategoria where megnevezes = 'Sci-Fi')),
       ('Dűne: Második rész', 166, (select kategoria_id from kategoria where megnevezes = 'Sci-Fi')),
       ('Oppenheimer', 180, (select kategoria_id from kategoria where megnevezes = 'Dráma')),
       ('Agymanók 2', 96, (select kategoria_id from kategoria where megnevezes = 'Animáció'));

insert into vetites (film_id, terem_id, kezdes_ido)
select f.film_id, t.terem_id, date_trunc('day', now()) + v.nap * interval '1 day' + v.ora * interval '1 hour'
from (values ('Mission Impossible', 'Nagyterem', 0, 20),
             ('Barbie', 'VIP Terem', 0, 18),
             ('Avatar: A víz útja', 'Nagyterem', 1, 17),
             ('Dűne: Második rész', 'Kisterem', 1, 19),
             ('Oppenheimer', 'Nagyterem', 2, 19),
             ('Agymanók 2', 'Kisterem', 2, 15),
             ('Barbie', 'Kisterem', 3, 18),
             ('Mission Impossible', 'VIP Terem', 3, 21),
             ('Dűne: Második rész', 'Nagyterem', -2, 19)) as v(cim, terem, nap, ora)
         join film f on f.cim = v.cim
         join terem t on t.nev = v.terem;

-- Tesztfelhasznalok. A jelszo hash BCrypt, a jelszavak a README-ben vannak.
insert into ugyfel (nev, email, jelszo_hash, szerep, torzsvasarloi_pontok)
values ('Admin', 'admin@mozi.hu', '$2a$10$WTxmJgJNPZjRAq4JVVe0WO9NFrJXEdKSsAG./pN50tW4Lz3sIz4K.', 'ADMIN', 0),
       ('Minta János', 'janos@example.com', '$2a$10$CVeepJoeMF0KZKDjhhWtXO04/djtOWbmWVtTrAumHF6ryLIUlY.Pi', 'UGYFEL', 10),
       ('Teszt Elek', 'elek@example.com', '$2a$10$082DhaG6k5paubepzbayEebLgySJxroJxI6MlJMFIHR92sav1HTcq', 'UGYFEL', 60);

-- Nehany eladott jegy, hogy legyen statisztika. A pontgyujto trigger itt is lefut.
insert into jegy (vetites_id, ugyfel_id, szek_szam, ar, status)
select v.vetites_id, u.ugyfel_id, j.szek, j.ar, j.status
from (values ('Dűne: Második rész', -2, 'janos@example.com', 10, 2500, 'FIZETVE'),
             ('Dűne: Második rész', -2, 'elek@example.com', 11, 2000, 'FIZETVE'),
             ('Mission Impossible', 0, 'elek@example.com', 50, 2000, 'FIZETVE'),
             ('Barbie', 0, 'janos@example.com', 5, 2500, 'FIZETVE'),
             ('Barbie', 0, 'janos@example.com', 6, 2500, 'TOROLT')) as j(cim, nap, email, szek, ar, status)
         join film f on f.cim = j.cim
         join vetites v on v.film_id = f.film_id and v.kezdes_ido::date = (current_date + j.nap)
         join ugyfel u on u.email = j.email;
