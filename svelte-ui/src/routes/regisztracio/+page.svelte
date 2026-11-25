<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { ApiHiba } from '#lib/api.ts';
	import { auth } from '#lib/auth.svelte.ts';

	let nev = $state('');
	let email = $state('');
	let jelszo = $state('');
	let jelszoUjra = $state('');
	let hiba = $state<string | null>(null);
	let mezok = $state<Record<string, string>>({});
	let folyamatban = $state(false);

	const vissza = $derived.by(() => {
		const cel = page.url.searchParams.get('vissza') ?? '/';
		return cel.startsWith('/') && !cel.startsWith('//') ? cel : '/';
	});

	async function bekuld(e: SubmitEvent) {
		e.preventDefault();
		hiba = null;
		mezok = {};
		// Ezt a backend nem kapja meg, ezert itt ellenorizzuk
		if (jelszo !== jelszoUjra) {
			mezok = { jelszoUjra: 'A két jelszó nem egyezik.' };
			return;
		}
		folyamatban = true;
		try {
			await auth.regisztracio(nev, email, jelszo);
			await goto(vissza);
		} catch (err) {
			if (err instanceof ApiHiba) {
				hiba = err.kod === 'validacio' ? null : err.message;
				mezok = err.mezok;
			} else {
				hiba = 'A regisztráció nem sikerült.';
			}
		} finally {
			folyamatban = false;
		}
	}
</script>

<svelte:head>
	<title>Regisztráció – Mozi Manager</title>
</svelte:head>

<div class="kartya doboz">
	<h1>Regisztráció</h1>
	<p class="halvany">Minden megvásárolt jegy után 10 pontot kapsz. 50 pont felett kedvezményes áron vásárolhatsz.</p>
	<form class="urlap" onsubmit={bekuld}>
		<label>
			Név
			<input bind:value={nev} autocomplete="name" required />
			{#if mezok.nev}<span class="mezo-hiba">{mezok.nev}</span>{/if}
		</label>
		<label>
			E-mail cím
			<input type="email" bind:value={email} autocomplete="email" required />
			{#if mezok.email}<span class="mezo-hiba">{mezok.email}</span>{/if}
		</label>
		<label>
			Jelszó (legalább 6 karakter)
			<input type="password" bind:value={jelszo} autocomplete="new-password" required />
			{#if mezok.jelszo}<span class="mezo-hiba">{mezok.jelszo}</span>{/if}
		</label>
		<label>
			Jelszó újra
			<input type="password" bind:value={jelszoUjra} autocomplete="new-password" required />
			{#if mezok.jelszoUjra}<span class="mezo-hiba">{mezok.jelszoUjra}</span>{/if}
		</label>
		{#if hiba}
			<div class="uzenet hiba">{hiba}</div>
		{/if}
		<button class="gomb" type="submit" disabled={folyamatban}>
			{folyamatban ? 'Regisztráció…' : 'Regisztrálok'}
		</button>
	</form>
	<p class="halvany">Van már fiókod? <a href="/belepes?vissza={vissza}">Lépj be!</a></p>
</div>

<style>
	.doboz {
		max-width: 420px;
		margin: 32px auto;
	}
</style>
