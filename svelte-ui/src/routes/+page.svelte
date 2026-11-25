<script lang="ts">
	import { api, ApiHiba } from '#lib/api.ts';
	import { idopont, napNeve } from '#lib/formatum.ts';
	import type { MusorElem } from '#lib/tipusok.ts';

	let musor = $state<MusorElem[]>([]);
	let betolt = $state(true);
	let hiba = $state<string | null>(null);

	// A vetitesek naponkent csoportositva, a backend mar idorendben kuldi oket
	let napok = $derived.by(() => {
		const csoportok = new Map<string, MusorElem[]>();
		for (const v of musor) {
			const kulcs = v.kezdesIdo.substring(0, 10);
			csoportok.set(kulcs, [...(csoportok.get(kulcs) ?? []), v]);
		}
		return [...csoportok.values()];
	});

	$effect(() => {
		api
			.musor()
			.then((m) => (musor = m))
			.catch((e) => (hiba = e instanceof ApiHiba ? e.message : 'Nem sikerült betölteni a műsort.'))
			.finally(() => (betolt = false));
	});

	const foglaltsag = (v: MusorElem) => 1 - v.szabadHelyek / v.kapacitas;
</script>

<svelte:head>
	<title>Műsor – Mozi Manager</title>
</svelte:head>

<h1>Műsor</h1>
<p class="halvany">Válassz vetítést, és foglald le a helyed.</p>

{#if betolt}
	<p class="halvany">Betöltés…</p>
{:else if hiba}
	<div class="uzenet hiba">{hiba}</div>
{:else if musor.length === 0}
	<div class="kartya">Jelenleg nincs meghirdetett vetítés.</div>
{:else}
	{#each napok as nap (nap[0].kezdesIdo)}
		<section>
			<h2>{napNeve(nap[0].kezdesIdo)}</h2>
			<div class="lista">
				{#each nap as v (v.vetitesId)}
					<a class="kartya vetites" href="/vetites/{v.vetitesId}">
						<span class="ido">{idopont(v.kezdesIdo)}</span>
						<span class="film">
							<strong>{v.filmCim}</strong>
							<span class="halvany">{v.kategoria} · {v.hosszPerc} perc · {v.terem}</span>
						</span>
						<span class="helyek" class:kevés={v.szabadHelyek > 0 && foglaltsag(v) > 0.8}>
							{#if v.szabadHelyek === 0}
								Telt ház
							{:else}
								{v.szabadHelyek} szabad hely
							{/if}
						</span>
					</a>
				{/each}
			</div>
		</section>
	{/each}
{/if}

<style>
	section {
		margin-top: 28px;
	}

	h2 {
		font-size: 1.05rem;
		color: var(--halvany);
		text-transform: none;
	}

	.lista {
		display: grid;
		gap: 10px;
	}

	.vetites {
		display: grid;
		grid-template-columns: 70px 1fr auto;
		align-items: center;
		gap: 16px;
		color: var(--szoveg);
		padding: 14px 18px;
		transition:
			border-color 0.15s,
			transform 0.15s;
	}

	.vetites:hover {
		text-decoration: none;
		border-color: var(--kiemelt);
		transform: translateY(-1px);
	}

	.ido {
		font-size: 1.3rem;
		font-weight: 700;
		color: var(--kiemelt);
	}

	.film {
		display: grid;
	}

	.film .halvany {
		font-size: 0.88rem;
	}

	.helyek {
		font-size: 0.9rem;
		color: var(--siker);
		white-space: nowrap;
	}

	.helyek.kevés {
		color: var(--kiemelt);
	}

	@media (max-width: 560px) {
		.vetites {
			grid-template-columns: 60px 1fr;
		}

		.helyek {
			grid-column: 2;
		}
	}
</style>
