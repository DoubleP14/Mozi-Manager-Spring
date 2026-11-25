# Mozi Manager – felhasználói felület

SvelteKit 3 (Svelte 5) egyoldalas alkalmazás. A Spring Boot backend REST API-ját használja.

## Indítás fejlesztéshez

Előbb fusson az adatbázis és a backend (lásd a fő README-t), utána:

```bash
npm install
npm run dev
```

Az oldal címe: http://localhost:5173

A `/api` kéréseket a Vite továbbítja a `http://localhost:8090` címre (`vite.config.ts`), így a böngésző számára minden egy helyről jön, és a session és a CSRF cookie gond nélkül működik.

## Parancsok

| Parancs | Mit csinál |
|---|---|
| `npm run dev` | Fejlesztői szerver |
| `npm run check` | Típusellenőrzés |
| `npm run build` | Statikus build a `build` mappába |

## Felépítés

| Fájl | Tartalom |
|---|---|
| `src/lib/api.ts` | A REST API hívásai, CSRF token, egységes hibakezelés |
| `src/lib/auth.svelte.ts` | A bejelentkezett felhasználó közös állapota |
| `src/lib/tipusok.ts` | A backend válaszainak típusai |
| `src/lib/formatum.ts` | Dátum-, ár- és székszám-formázás |
| `src/routes/+page.svelte` | Műsor |
| `src/routes/vetites/[id]/+page.svelte` | Székválasztás és vásárlás |
| `src/routes/belepes`, `regisztracio` | Belépés, regisztráció |
| `src/routes/jegyeim/+page.svelte` | Saját jegyek, lemondás |
