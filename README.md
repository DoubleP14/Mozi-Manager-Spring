# Mozi Manager - Spring Boot alkalmazás

Ez a repository a **Mozi Manager** projekt Spring Boot alkalmazását tartalmazza, a Programozás 5 tárgy beadandójaként. A projekt az Adatbázis 2 tárgyra készült, Oracle PL/SQL alapú [Mozi-Manager-PLSQL](https://github.com/DoubleP14/Mozi-Manager-PLSQL) adatbázis-réteg továbbfejlesztése: az adatbázis PostgreSQL-re került, és köré egy teljes webalkalmazás épül jegyvásárlással, teremkezeléssel és statisztikákkal.

## Telepítési útmutató

### Előfeltételek

* **Java 25**
* **Docker Desktop** (az adatbázishoz)

A Maven-t nem kell telepíteni, a projekt tartalmazza a Maven Wrappert (`mvnw`).

### 1. Adatbázis indítása

```bash
cd development/docker
docker compose up -d
```

Ez elindít egy PostgreSQL 17 konténert (`localhost:5432`, adatbázis, felhasználó és jelszó: `mozi`).

### 2. Alkalmazás indítása

A projekt gyökeréből:

```bash
./mvnw spring-boot:run
```

Az alkalmazás címe: http://localhost:8090

* **Admin felület (Vaadin):** http://localhost:8090/admin, belépés az `admin@mozi.hu` felhasználóval
* **REST API (a Svelte felületnek):** http://localhost:8090/api

Első indításkor a Vaadin előkészíti a frontendet, ez eltarthat egy-két percig.

### Az adatbázis felépítése

Az Oracle változattal ellentétben a szkripteket nem kell kézzel futtatni. Az első induláskor a PostgreSQL konténer az alábbi sorrendben automatikusan lefuttatja a `development/docker/init` mappa fájljait:

#### 1. Táblák
* `1-schema.sql` (Táblák, history táblák, hibanapló, megszorítások és az egyedi székfoglalási index.)

#### 2. Függvények és triggerek
* `2-functions.sql` (Audit mezők és naplózás a `*_history` táblákba, törzsvásárlói pontgyűjtés, székszám-ellenőrzés, `szabad_helyek` függvény.)

#### 3. Nézetek
* `3-views.sql` (Műsorújság a szabad helyekkel és bevételek filmenként.)

#### 4. Adatok feltöltése
* `4-data.sql` (Kezdeti adatok: kategóriák, termek, filmek, a mai naphoz igazított vetítések, ügyfelek és jegyek.)

#### Az adatbázis újratöltése

A szkriptek csak az első indításkor futnak le. Módosításuk után, vagy a kezdeti adatok visszaállításához:

```bash
docker compose down -v
docker compose up -d
```

A `-v` az adatbázis teljes tartalmát törli.

---

## Tesztek futtatása

```bash
./mvnw clean package
```

Az adatbázisos tesztek Testcontainers segítségével saját, eldobható PostgreSQL konténert indítanak ugyanazokkal az `init` szkriptekkel, így a fejlesztői adatbázist nem módosítják. Futó Docker kell hozzájuk, Docker nélkül kimaradnak.

* `JegyServiceTest` (Jegyvásárlás alap és kedvezményes áron, pontjóváírás, foglalt és nem létező szék, elmúlt vetítés, lemondás, hibanaplózás külön tranzakcióban, a bejelentkezett felhasználó naplózása, bevételi nézet és a `szabad_helyek` függvény.)
* `AuthApiTest` (Belépés, hibás jelszó, regisztráció BCrypt hash-sel, foglalt e-mail cím, validáció, kijelentkezés, CSRF védelem, admin jogosultság.)
* `AdminServiceTest` (Az admin műveletek jogosultsága, átfedő vetítés, múltbeli vetítés, használatban lévő kategória, jegyes vetítés terme és törlése, terem kapacitása.)

---

## Admin felület

A Vaadin admin felület a `/admin` alatt fut, és csak `ADMIN` szerepű felhasználó használhatja.

| Menüpont | Funkció |
|---|---|
| Vetítések | Vetítések felvétele, módosítása, törlése, foglaltsággal |
| Filmek | Filmek kezelése kategóriával |
| Termek | Termek és kapacitásuk |
| Kategóriák | Filmkategóriák |
| Statisztika | Bevétel filmenként (`v_bevetel_filmenkent` nézet) |
| Hibanapló | A `hiba_naplo` tábla utolsó 100 bejegyzése |

Üzleti szabályok:

* Egy teremben nem lehet időben átfedő vetítés (a film hossza és 15 perc takarítás).
* Új vetítés nem lehet a múltban.
* Eladott jeggyel rendelkező vetítés terme nem módosítható, és a vetítés nem törölhető.
* Terem kapacitása nem csökkenthető a már eladott legnagyobb székszám alá.
* Használatban lévő kategória, terem és film nem törölhető.

A felület magyarul és angolul is használható, a nyelv a fejlécben váltható.

---

## REST API

A Svelte felület ezeken a végpontokon keresztül éri el a backendet. A módosító kérésekhez (POST) CSRF token kell: a `GET /api/auth/csrf` beállítja az `XSRF-TOKEN` cookie-t, amelynek értékét az `X-XSRF-TOKEN` fejlécben kell visszaküldeni.

| Végpont | Leírás | Jogosultság |
|---|---|---|
| `GET /api/auth/csrf` | CSRF token | Mindenki |
| `POST /api/auth/bejelentkezes` | Belépés (`email`, `jelszo`) | Mindenki |
| `POST /api/auth/regisztracio` | Regisztráció (`nev`, `email`, `jelszo`), utána belépteti | Mindenki |
| `GET /api/auth/en` | A bejelentkezett felhasználó | Bejelentkezett |
| `POST /api/auth/kijelentkezes` | Kijelentkezés | Bejelentkezett |

Hibánál a válasz egységes: `{ "kod": "...", "uzenet": "...", "mezok": { ... } }`.

---

## Tesztfelhasználók

| E-mail | Jelszó | Szerep |
|---|---|---|
| `admin@mozi.hu` | `admin` | ADMIN |
| `janos@example.com` | `janos` | UGYFEL |
| `elek@example.com` | `elek` | UGYFEL |

---

## Felépítés

```text
mozi-manager/
├── development/docker/        PostgreSQL konténer és init szkriptek
└── src/main/java/hu/mik/pte/prog5/mozi/
    ├── api/                   REST végpontok és egységes hibakezelés
    ├── security/              Spring Security beállítás, felhasználók az adatbázisból
    ├── ui/                    Vaadin admin felület (keret, belépés, nézetek)
    ├── model/                 entitások és a nézetek recordjai
    ├── repository/            Spring Data JDBC repositoryk, JdbcTemplate lekérdezések
    ├── service/               üzleti logika (jegyvásárlás, lemondás, hibanapló)
    └── config/                beállítások
```

---

## 🛠️ Használt technológiák

* **Backend:** Spring Boot 4.1, Java 25
* **Adatelérés:** Spring Data JDBC, JdbcTemplate, SimpleJdbcCall
* **Biztonság:** Spring Security (session alapú belépés, BCrypt, CSRF védelem, method security)
* **Admin felület:** Vaadin 25 (magyar és angol nyelven)
* **Adatbázis:** PostgreSQL 17, PL/pgSQL (Docker)
* **Tesztek:** JUnit 5, MockMvc, Testcontainers
* **Eszközök:** IntelliJ IDEA, Docker Desktop
* **Project Status:** Fejlesztés alatt

---

## 👤 Szerző

**Név:** [Pintér Patrik]  
**Neptun kód:** [IEKOO6]
