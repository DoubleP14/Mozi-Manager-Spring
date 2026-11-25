// A backend JSON valaszainak tipusai (a Java recordok mezoi)

export type Szerep = 'UGYFEL' | 'ADMIN';
export type JegyStatus = 'FOGLALT' | 'FIZETVE' | 'TOROLT';

export interface Felhasznalo {
	ugyfelId: number;
	nev: string;
	email: string;
	szerep: Szerep;
	torzsvasarloiPontok: number;
}

// A v_musor nezet egy sora
export interface MusorElem {
	vetitesId: number;
	filmId: number;
	filmCim: string;
	kategoria: string;
	teremId: number;
	terem: string;
	kapacitas: number;
	kezdesIdo: string;
	hosszPerc: number;
	szabadHelyek: number;
}

export interface VetitesReszletek {
	vetites: MusorElem;
	foglaltSzekek: number[];
	egysegar: number;
}

export interface VasarlasEredmeny {
	jegyIdk: number[];
	szekek: number[];
	egysegar: number;
	osszeg: number;
	torzsvasarloiPontok: number;
}

export interface SajatJegy {
	jegyId: number;
	vetitesId: number;
	filmCim: string;
	terem: string;
	kezdesIdo: string;
	szekSzam: number;
	ar: number;
	status: JegyStatus;
}
