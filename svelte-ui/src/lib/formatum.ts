const ido = new Intl.DateTimeFormat('hu-HU', { hour: '2-digit', minute: '2-digit' });
const nap = new Intl.DateTimeFormat('hu-HU', { month: 'long', day: 'numeric', weekday: 'long' });
const teljes = new Intl.DateTimeFormat('hu-HU', {
	year: 'numeric',
	month: 'long',
	day: 'numeric',
	hour: '2-digit',
	minute: '2-digit'
});
const penz = new Intl.NumberFormat('hu-HU');

// A backend "2026-11-26T18:00:00" formaban kuldi az idopontot (helyi ido, idozona nelkul)
export const datum = (s: string) => new Date(s);

export const idopont = (s: string) => ido.format(datum(s));
export const teljesIdopont = (s: string) => teljes.format(datum(s));
export const forint = (n: number) => `${penz.format(n)} Ft`;

// "Ma", "Holnap", vagy a nap neve es datuma
export function napNeve(s: string): string {
	const d = datum(s);
	const ma = new Date();
	ma.setHours(0, 0, 0, 0);
	const kulonbseg = Math.round((new Date(d).setHours(0, 0, 0, 0) - ma.getTime()) / 86_400_000);
	if (kulonbseg === 0) return 'Ma';
	if (kulonbseg === 1) return 'Holnap';
	const szoveg = nap.format(d);
	return szoveg.charAt(0).toUpperCase() + szoveg.slice(1);
}

// Szekszambol sor es hely: a sorok merete a terem kapacitasatol fugg
export function sorHossz(kapacitas: number): number {
	if (kapacitas >= 100) return 12;
	if (kapacitas >= 50) return 10;
	return 6;
}

export function szekNeve(szekSzam: number, kapacitas: number): string {
	const hossz = sorHossz(kapacitas);
	const sor = String.fromCharCode(65 + Math.floor((szekSzam - 1) / hossz));
	return `${sor} sor ${((szekSzam - 1) % hossz) + 1}. szék`;
}
