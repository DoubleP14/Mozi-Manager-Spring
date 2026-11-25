import type {
	Felhasznalo,
	MusorElem,
	SajatJegy,
	VasarlasEredmeny,
	VetitesReszletek
} from './tipusok';

/**
 * A backend hibavalasza: { kod, uzenet, mezok }.
 * A mezok validacios hibanal tartalmazzak a hibas mezoket.
 */
export class ApiHiba extends Error {
	constructor(
		public readonly status: number,
		public readonly kod: string,
		uzenet: string,
		public readonly mezok: Record<string, string> = {}
	) {
		super(uzenet);
	}
}

const CSRF_COOKIE = 'XSRF-TOKEN';

function cookie(nev: string): string | null {
	const sor = document.cookie.split('; ').find((c) => c.startsWith(nev + '='));
	const ertek = sor?.substring(nev.length + 1);
	return ertek ? decodeURIComponent(ertek) : null;
}

/**
 * A CSRF token a backend altal beallitott XSRF-TOKEN cookie-ban van.
 * Ha meg nincs (vagy kijelentkezes utan torlodott), elobb lekerjuk.
 */
async function csrfToken(): Promise<string> {
	let token = cookie(CSRF_COOKIE);
	if (!token) {
		await fetch('/api/auth/csrf', { credentials: 'same-origin' });
		token = cookie(CSRF_COOKIE);
	}
	if (!token) {
		throw new ApiHiba(0, 'csrf', 'Nem sikerült biztonsági tokent kérni a szervertől.');
	}
	return token;
}

async function keres<T>(url: string, opciok: { metodus?: string; adat?: unknown } = {}): Promise<T> {
	const metodus = opciok.metodus ?? 'GET';
	const fejlecek: Record<string, string> = { Accept: 'application/json' };
	if (opciok.adat !== undefined) {
		fejlecek['Content-Type'] = 'application/json';
	}
	// A modosito kereseknel kotelezo a CSRF token fejlecben
	if (metodus !== 'GET') {
		fejlecek['X-XSRF-TOKEN'] = await csrfToken();
	}

	let valasz: Response;
	try {
		valasz = await fetch(url, {
			method: metodus,
			headers: fejlecek,
			credentials: 'same-origin',
			body: opciok.adat === undefined ? undefined : JSON.stringify(opciok.adat)
		});
	} catch {
		throw new ApiHiba(0, 'halozat', 'A szerver nem érhető el. Fut a backend?');
	}

	if (!valasz.ok) {
		const hiba = await valasz.json().catch(() => null);
		throw new ApiHiba(
			valasz.status,
			hiba?.kod ?? 'ismeretlen',
			hiba?.uzenet ?? (valasz.status === 401 ? 'Ehhez be kell jelentkezned.' : 'Váratlan hiba történt.'),
			hiba?.mezok ?? {}
		);
	}
	if (valasz.status === 204) {
		return undefined as T;
	}
	return (await valasz.json()) as T;
}

export const api = {
	en: () => keres<Felhasznalo>('/api/auth/en'),
	bejelentkezes: (email: string, jelszo: string) =>
		keres<Felhasznalo>('/api/auth/bejelentkezes', { metodus: 'POST', adat: { email, jelszo } }),
	regisztracio: (nev: string, email: string, jelszo: string) =>
		keres<Felhasznalo>('/api/auth/regisztracio', { metodus: 'POST', adat: { nev, email, jelszo } }),
	kijelentkezes: () => keres<void>('/api/auth/kijelentkezes', { metodus: 'POST' }),

	musor: () => keres<MusorElem[]>('/api/musor'),
	vetites: (vetitesId: number) => keres<VetitesReszletek>(`/api/musor/${vetitesId}`),

	sajatJegyek: () => keres<SajatJegy[]>('/api/jegyek'),
	vasarlas: (vetitesId: number, szekek: number[]) =>
		keres<VasarlasEredmeny>('/api/jegyek', { metodus: 'POST', adat: { vetitesId, szekek } }),
	lemondas: (jegyId: number) => keres<void>(`/api/jegyek/${jegyId}/lemondas`, { metodus: 'POST' })
};
