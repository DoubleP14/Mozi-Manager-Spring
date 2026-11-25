<script lang="ts">
	import { page } from '$app/state';
	import { api, ApiHiba } from '#lib/api.ts';
	import { auth } from '#lib/auth.svelte.ts';
	import { forint, napNeve, idopont, sorHossz, szekNeve } from '#lib/formatum.ts';
	import type { VasarlasEredmeny, VetitesReszletek } from '#lib/tipusok.ts';

	const MAX_SZEK = 10;

	const vetitesId = $derived(Number(page.params.id));

	let reszletek = $state<VetitesReszletek | null>(null);
	let kivalasztott = $state<number[]>([]);
	let hiba = $state<string | null>(null);
	let eredmeny = $state<VasarlasEredmeny | null>(null);
	let folyamatban = $state(false);

	// A szekek sorokra bontva; a sor hossza a terem meretetol fugg
	let sorok = $derived.by(() => {
		if (!reszletek) return [];
		const kapacitas = reszletek.vetites.kapacitas;
		const hossz = sorHossz(kapacitas);
		const eredmeny: number[][] = [];
		for (let elso = 1; elso <= kapacitas; elso += hossz) {
			eredmeny.push(Array.from({ length: Math.min(hossz, kapacitas - elso + 1) }, (_, i) => elso + i));
		}
		return eredmeny;
	});

	let foglalt = $derived(new Set(reszletek?.foglaltSzekek ?? []));
	let osszeg = $derived((reszletek?.egysegar ?? 0) * kivalasztott.length);

	async function betolt() {
		try {
			reszletek = await api.vetites(vetitesId);
			// Ha kozben valaki mas megvette valamelyik kivalasztott szeket, azt kivesszuk
			kivalasztott = kivalasztott.filter((s) => !reszletek!.foglaltSzekek.includes(s));
		} catch (e) {
			hiba = e instanceof ApiHiba ? e.message : 'Nem sikerült betölteni a vetítést.';
		}
	}

	// Ujratolt, ha a bejelentkezes valtozik (az ar a pontoktol fugg)
	$effect(() => {
		auth.felhasznalo?.ugyfelId;
		betolt();
	});

	function valt(szek: number) {
		eredmeny = null;
		if (kivalasztott.includes(szek)) {
			kivalasztott = kivalasztott.filter((s) => s !== szek);
		} else if (kivalasztott.length < MAX_SZEK) {
			kivalasztott = [...kivalasztott, szek].sort((a, b) => a - b);
		}
	}

	async function vasarlas() {
		if (!reszletek || kivalasztott.length === 0) return;
		folyamatban = true;
		hiba = null;
		try {
			eredmeny = await api.vasarlas(vetitesId, kivalasztott);
			auth.pontokFrissitese(eredmeny.torzsvasarloiPontok);
			kivalasztott = [];
		} catch (e) {
			hiba = e instanceof ApiHiba ? e.message : 'A vásárlás nem sikerült.';
		} finally {
			folyamatban = false;
			await betolt();
		}
	}
</script>

<svelte:head>
	<title>{reszletek ? reszletek.vetites.filmCim : 'Vetítés'} – Mozi Manager</title>
</svelte:head>

<p><a href="/">← Vissza a műsorhoz</a></p>

{#if !reszletek && hiba}
	<div class="uzenet hiba">{hiba}</div>
{:else if !reszletek}
	<p class="halvany">Betöltés…</p>
{:else}
	{@const v = reszletek.vetites}
	<h1>{v.filmCim}</h1>
	<p class="halvany">
		{napNeve(v.kezdesIdo)}, {idopont(v.kezdesIdo)} · {v.terem} · {v.kategoria} · {v.hosszPerc} perc
	</p>

	<div class="elrendezes">
		<div class="kartya terem">
			<div class="vaszon">VÁSZON</div>
			<div class="szekek" style="--oszlopok: {sorHossz(v.kapacitas)}">
				{#each sorok as sor, i (i)}
					<span class="sorjel">{String.fromCharCode(65 + i)}</span>
					{#each sor as szek (szek)}
						<button
							class="szek"
							class:foglalt={foglalt.has(szek)}
							class:kivalasztott={kivalasztott.includes(szek)}
							disabled={foglalt.has(szek)}
							title={szekNeve(szek, v.kapacitas)}
							aria-label={szekNeve(szek, v.kapacitas)}
							aria-pressed={kivalasztott.includes(szek)}
							onclick={() => valt(szek)}
						>
							{((szek - 1) % sorHossz(v.kapacitas)) + 1}
						</button>
					{/each}
				{/each}
			</div>
			<div class="jelmagyarazat">
				<span><i class="minta szabad"></i>Szabad</span>
				<span><i class="minta kivalasztott"></i>Kiválasztott</span>
				<span><i class="minta foglalt"></i>Foglalt</span>
			</div>
		</div>

		<aside class="kartya osszesito">
			<h2>Kosár</h2>
			{#if kivalasztott.length === 0}
				<p class="halvany">Kattints a szabad székekre. Egyszerre legfeljebb {MAX_SZEK} jegyet vehetsz.</p>
			{:else}
				<ul>
					{#each kivalasztott as szek (szek)}
						<li>{szekNeve(szek, v.kapacitas)}</li>
					{/each}
				</ul>
			{/if}

			<p class="ar">
				{kivalasztott.length} × {forint(reszletek.egysegar)}
				<strong>{forint(osszeg)}</strong>
			</p>
			{#if reszletek.egysegar < 2500}
				<p class="cimke">Törzsvásárlói kedvezményes ár</p>
			{/if}

			{#if auth.felhasznalo}
				<button class="gomb teljes" disabled={kivalasztott.length === 0 || folyamatban} onclick={vasarlas}>
					{folyamatban ? 'Vásárlás…' : 'Jegyek megvásárlása'}
				</button>
			{:else}
				<a class="gomb teljes" href="/belepes?vissza=/vetites/{vetitesId}">Belépés a vásárláshoz</a>
			{/if}

			{#if hiba}
				<div class="uzenet hiba">{hiba}</div>
			{/if}
			{#if eredmeny}
				<div class="uzenet siker">
					Sikeres vásárlás: {eredmeny.szekek.length} jegy, {forint(eredmeny.osszeg)}.<br />
					Pontjaid: {eredmeny.torzsvasarloiPontok}. <a href="/jegyeim">Jegyeim →</a>
				</div>
			{/if}
		</aside>
	</div>
{/if}

<style>
	.elrendezes {
		display: grid;
		grid-template-columns: 1fr 280px;
		gap: 20px;
		align-items: start;
		margin-top: 20px;
	}

	.terem {
		overflow-x: auto;
	}

	.vaszon {
		text-align: center;
		letter-spacing: 0.4em;
		font-size: 0.8rem;
		color: var(--halvany);
		padding-bottom: 10px;
		margin: 0 10% 24px;
		border-bottom: 3px solid var(--kiemelt);
		border-radius: 0 0 50% 50% / 0 0 14px 14px;
	}

	.szekek {
		display: grid;
		grid-template-columns: 24px repeat(var(--oszlopok), minmax(30px, 42px));
		gap: 6px;
		/* width + margin: kozepen van, de ha nem fer ki, balrol nem vagodik le, hanem gorgetheto */
		width: max-content;
		margin: 0 auto;
	}

	.sorjel {
		align-self: center;
		color: var(--halvany);
		font-size: 0.8rem;
		text-align: center;
	}

	.szek {
		aspect-ratio: 1;
		min-width: 30px;
		border-radius: 6px 6px 3px 3px;
		border: 1px solid var(--keret);
		background: var(--felulet-2);
		color: var(--halvany);
		font-size: 0.72rem;
		cursor: pointer;
	}

	.szek:hover:not(:disabled) {
		border-color: var(--kiemelt);
		color: var(--szoveg);
	}

	.szek.kivalasztott {
		background: var(--kiemelt);
		border-color: var(--kiemelt);
		color: #111;
		font-weight: 700;
	}

	.szek.foglalt {
		background: var(--foglalt);
		color: transparent;
		cursor: not-allowed;
	}

	.jelmagyarazat {
		display: flex;
		gap: 18px;
		justify-content: center;
		margin-top: 20px;
		font-size: 0.85rem;
		color: var(--halvany);
	}

	.minta {
		display: inline-block;
		width: 14px;
		height: 14px;
		border-radius: 3px;
		margin-right: 6px;
		vertical-align: -2px;
		border: 1px solid var(--keret);
	}

	.minta.szabad {
		background: var(--felulet-2);
	}

	.minta.kivalasztott {
		background: var(--kiemelt);
	}

	.minta.foglalt {
		background: var(--foglalt);
	}

	.osszesito {
		position: sticky;
		top: 80px;
	}

	.osszesito ul {
		padding-left: 18px;
		margin: 0 0 12px;
	}

	.ar {
		display: flex;
		justify-content: space-between;
		border-top: 1px solid var(--keret);
		padding-top: 12px;
	}

	.ar strong {
		font-size: 1.2rem;
	}

	.teljes {
		width: 100%;
		margin-top: 8px;
	}

	@media (max-width: 760px) {
		.elrendezes {
			grid-template-columns: 1fr;
		}

		.osszesito {
			position: static;
		}
	}

	/* Telefonon kisebb szekek, hogy a 12 szekes sor is kiferjen */
	@media (max-width: 560px) {
		.terem {
			padding: 12px 8px;
		}

		.szekek {
			grid-template-columns: 16px repeat(var(--oszlopok), 22px);
			gap: 3px;
		}

		.szek {
			min-width: 22px;
			font-size: 0.6rem;
		}

		.sorjel {
			font-size: 0.7rem;
		}
	}
</style>
