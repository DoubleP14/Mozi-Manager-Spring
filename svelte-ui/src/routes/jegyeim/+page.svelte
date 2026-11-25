<script lang="ts">
	import { goto } from '$app/navigation';
	import { api, ApiHiba } from '#lib/api.ts';
	import { auth } from '#lib/auth.svelte.ts';
	import { datum, forint, teljesIdopont } from '#lib/formatum.ts';
	import type { SajatJegy } from '#lib/tipusok.ts';

	let jegyek = $state<SajatJegy[]>([]);
	let betolt = $state(true);
	let hiba = $state<string | null>(null);
	let uzenet = $state<string | null>(null);

	const STATUSZ: Record<string, string> = { FIZETVE: 'Fizetve', FOGLALT: 'Foglalt', TOROLT: 'Lemondva' };

	// A backend a legujabbal kezdi; a kozelgo vetiteseknel a legkorabbi legyen elol
	let jovobeli = $derived(
		jegyek
			.filter((j) => datum(j.kezdesIdo) > new Date())
			.sort((a, b) => a.kezdesIdo.localeCompare(b.kezdesIdo) || a.szekSzam - b.szekSzam)
	);
	let korabbi = $derived(jegyek.filter((j) => datum(j.kezdesIdo) <= new Date()));

	async function frissit() {
		try {
			jegyek = await api.sajatJegyek();
		} catch (e) {
			hiba = e instanceof ApiHiba ? e.message : 'Nem sikerült betölteni a jegyeket.';
		} finally {
			betolt = false;
		}
	}

	// Bejelentkezes nelkul ez az oldal nem ertelmes: a belepesre kuldjuk
	$effect(() => {
		if (!auth.betoltve) return;
		if (!auth.felhasznalo) {
			goto('/belepes?vissza=/jegyeim');
			return;
		}
		frissit();
	});

	async function lemondas(jegy: SajatJegy) {
		if (!confirm(`Biztosan lemondod a(z) ${jegy.szekSzam}. székre szóló jegyet (${jegy.filmCim})?`)) {
			return;
		}
		hiba = null;
		uzenet = null;
		try {
			await api.lemondas(jegy.jegyId);
			uzenet = 'A jegyet lemondtad, a szék újra szabad.';
			await frissit();
		} catch (e) {
			hiba = e instanceof ApiHiba ? e.message : 'A lemondás nem sikerült.';
		}
	}
</script>

<svelte:head>
	<title>Jegyeim – Mozi Manager</title>
</svelte:head>

<h1>Jegyeim</h1>
{#if auth.felhasznalo}
	<p class="halvany">Törzsvásárlói pontjaid: <strong>{auth.felhasznalo.torzsvasarloiPontok}</strong></p>
{/if}

{#if hiba}<div class="uzenet hiba">{hiba}</div>{/if}
{#if uzenet}<div class="uzenet siker">{uzenet}</div>{/if}

{#snippet tablazat(lista: SajatJegy[], lemondhato: boolean)}
	<div class="lista">
		{#each lista as j (j.jegyId)}
			<div class="kartya jegy" class:torolt={j.status === 'TOROLT'}>
				<div>
					<strong>{j.filmCim}</strong>
					<div class="halvany">{teljesIdopont(j.kezdesIdo)} · {j.terem} · {j.szekSzam}. szék</div>
				</div>
				<div class="jobb">
					<span class="cimke">{STATUSZ[j.status]}</span>
					<span>{forint(j.ar)}</span>
					{#if lemondhato && j.status === 'FIZETVE'}
						<button class="gomb veszely" onclick={() => lemondas(j)}>Lemondás</button>
					{/if}
				</div>
			</div>
		{/each}
	</div>
{/snippet}

{#if betolt}
	<p class="halvany">Betöltés…</p>
{:else if jegyek.length === 0}
	<div class="kartya">Még nincs jegyed. <a href="/">Nézd meg a műsort!</a></div>
{:else}
	<h2>Közelgő vetítések</h2>
	{#if jovobeli.length === 0}
		<p class="halvany">Nincs közelgő vetítésre szóló jegyed.</p>
	{:else}
		{@render tablazat(jovobeli, true)}
	{/if}

	{#if korabbi.length > 0}
		<h2>Korábbi jegyek</h2>
		{@render tablazat(korabbi, false)}
	{/if}
{/if}

<style>
	h2 {
		margin-top: 28px;
		font-size: 1.05rem;
		color: var(--halvany);
	}

	.lista {
		display: grid;
		gap: 10px;
	}

	.jegy {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 16px;
		flex-wrap: wrap;
		padding: 14px 18px;
	}

	.jegy.torolt {
		opacity: 0.55;
	}

	.jobb {
		display: flex;
		align-items: center;
		gap: 14px;
	}

	.jobb .gomb {
		padding: 6px 12px;
	}
</style>
