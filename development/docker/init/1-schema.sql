-- Mozi Manager - tablak
-- Az Adatbazis 2 Oracle semajanak PostgreSQL valtozata.
--   NUMBER        -> bigint / integer
--   VARCHAR2      -> varchar
--   DATE          -> timestamp
--   szekvencia    -> generated always as identity (a kezdoertekek az eredetiek)

drop table if exists jegy_history, ugyfel_history, vetites_history, film_history, terem_history,
    kategoria_history, hiba_naplo, jegy, ugyfel, vetites, film, terem, kategoria cascade;

-- 1. KATEGORIA
create table kategoria
(
    kategoria_id bigint generated always as identity (start with 1) primary key,
    megnevezes   varchar(50) not null unique,
    letrehozva   timestamp,
    letrehozta   varchar(100),
    modositva    timestamp,
    modositotta  varchar(100)
);

create table kategoria_history
(
    kategoria_id         bigint,
    megnevezes           varchar(50),
    muvelet              varchar(10),
    modositas_ideje      timestamp,
    modosito_felhasznalo varchar(100)
);

-- 2. TEREM
create table terem
(
    terem_id    bigint generated always as identity (start with 1) primary key,
    nev         varchar(50) not null unique,
    kapacitas   integer     not null check (kapacitas > 0),
    letrehozva  timestamp,
    letrehozta  varchar(100),
    modositva   timestamp,
    modositotta varchar(100)
);

create table terem_history
(
    terem_id             bigint,
    nev                  varchar(50),
    kapacitas            integer,
    muvelet              varchar(10),
    modositas_ideje      timestamp,
    modosito_felhasznalo varchar(100)
);

-- 3. FILM
create table film
(
    film_id      bigint generated always as identity (start with 10000) primary key,
    cim          varchar(100) not null,
    hossz_perc   integer      not null check (hossz_perc > 0),
    kategoria_id bigint       not null references kategoria (kategoria_id),
    letrehozva   timestamp,
    letrehozta   varchar(100),
    modositva    timestamp,
    modositotta  varchar(100)
);

create table film_history
(
    film_id              bigint,
    cim                  varchar(100),
    hossz_perc           integer,
    kategoria_id         bigint,
    muvelet              varchar(10),
    modositas_ideje      timestamp,
    modosito_felhasznalo varchar(100)
);

-- 4. VETITES
create table vetites
(
    vetites_id  bigint generated always as identity (start with 10000) primary key,
    film_id     bigint    not null references film (film_id),
    terem_id    bigint    not null references terem (terem_id),
    kezdes_ido  timestamp not null,
    letrehozva  timestamp,
    letrehozta  varchar(100),
    modositva   timestamp,
    modositotta varchar(100)
);

create table vetites_history
(
    vetites_id           bigint,
    film_id              bigint,
    terem_id             bigint,
    kezdes_ido           timestamp,
    muvelet              varchar(10),
    modositas_ideje      timestamp,
    modosito_felhasznalo varchar(100)
);

-- 5. UGYFEL
-- Uj a Prog5 valtozatban: jelszo_hash es szerep, mert az ugyfelek be is jelentkeznek.
create table ugyfel
(
    ugyfel_id            bigint generated always as identity (start with 10000) primary key,
    nev                  varchar(100) not null,
    email                varchar(100) not null unique,
    jelszo_hash          varchar(100) not null,
    szerep               varchar(20)  not null default 'UGYFEL' check (szerep in ('UGYFEL', 'ADMIN')),
    torzsvasarloi_pontok integer      not null default 0,
    letrehozva           timestamp,
    letrehozta           varchar(100),
    modositva            timestamp,
    modositotta          varchar(100)
);

-- A jelszo hash-et szandekosan nem naplozzuk
create table ugyfel_history
(
    ugyfel_id            bigint,
    nev                  varchar(100),
    email                varchar(100),
    szerep               varchar(20),
    torzsvasarloi_pontok integer,
    muvelet              varchar(10),
    modositas_ideje      timestamp,
    modosito_felhasznalo varchar(100)
);

-- 6. JEGY
create table jegy
(
    jegy_id     bigint generated always as identity (start with 10000) primary key,
    vetites_id  bigint      not null references vetites (vetites_id),
    ugyfel_id   bigint      not null references ugyfel (ugyfel_id),
    szek_szam   integer     not null check (szek_szam > 0),
    ar          integer     not null check (ar >= 0),
    status      varchar(20) not null check (status in ('FOGLALT', 'FIZETVE', 'TOROLT')),
    letrehozva  timestamp,
    letrehozta  varchar(100),
    modositva   timestamp,
    modositotta varchar(100)
);

-- Uj: egy vetitesen egy szekre csak egy ervenyes (nem torolt) jegy lehet.
-- Ez az adatbazis szintjen is megakadalyozza a dupla foglalast, ket egyideju vasarlasnal is.
create unique index ux_jegy_vetites_szek on jegy (vetites_id, szek_szam) where status <> 'TOROLT';

create table jegy_history
(
    jegy_id              bigint,
    vetites_id           bigint,
    ugyfel_id            bigint,
    szek_szam            integer,
    ar                   integer,
    status               varchar(20),
    muvelet              varchar(10),
    modositas_ideje      timestamp,
    modosito_felhasznalo varchar(100)
);

-- 7. HIBA NAPLO
create table hiba_naplo
(
    hiba_id      bigint generated always as identity primary key,
    hiba_kod     varchar(50),
    hiba_uzenet  varchar(4000),
    eljaras_neve varchar(100),
    idopont      timestamp not null default now(),
    felhasznalo  varchar(100)
);
