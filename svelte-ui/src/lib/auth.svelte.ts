import { api, ApiHiba } from './api';
import type { Felhasznalo } from './tipusok';

/**
 * A bejelentkezett felhasznalo, az egesz alkalmazasban kozos allapot.
 * A $state miatt minden komponens, ami olvassa, magatol frissul, ha valtozik.
 */
class AuthAllapot {
	felhasznalo = $state<Felhasznalo | null>(null);
	betoltve = $state(false);

	// Az oldal betoltesekor: a session cookie alapjan a backend megmondja, be vagyunk-e jelentkezve
	async betolt() {
		try {
			this.felhasznalo = await api.en();
		} catch (e) {
			if (!(e instanceof ApiHiba) || e.status !== 401) {
				console.error(e);
			}
			this.felhasznalo = null;
		} finally {
			this.betoltve = true;
		}
	}

	async bejelentkezes(email: string, jelszo: string) {
		this.felhasznalo = await api.bejelentkezes(email, jelszo);
	}

	async regisztracio(nev: string, email: string, jelszo: string) {
		this.felhasznalo = await api.regisztracio(nev, email, jelszo);
	}

	async kijelentkezes() {
		await api.kijelentkezes();
		this.felhasznalo = null;
	}

	// Vasarlas utan a pontszam valtozik (a trigger irja jova)
	pontokFrissitese(pontok: number) {
		if (this.felhasznalo) {
			this.felhasznalo.torzsvasarloiPontok = pontok;
		}
	}
}

export const auth = new AuthAllapot();
