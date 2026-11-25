<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { ApiHiba } from '#lib/api.ts';
	import { auth } from '#lib/auth.svelte.ts';

	let email = $state('');
	let jelszo = $state('');
	let hiba = $state<string | null>(null);
	let folyamatban = $state(false);

	// Belepes utan oda megyunk vissza, ahonnan jott (pl. a szekvalasztohoz). Csak sajat oldalra.
	const vissza = $derived.by(() => {
		const cel = page.url.searchParams.get('vissza') ?? '/';
		return cel.startsWith('/') && !cel.startsWith('//') ? cel : '/';
	});

	async function bekuld(e: SubmitEvent) {
		e.preventDefault();
		folyamatban = true;
		hiba = null;
		try {
			await auth.bejelentkezes(email, jelszo);
			await goto(vissza);
		} catch (err) {
			hiba = err instanceof ApiHiba ? err.message : 'A belépés nem sikerült.';
		} finally {
			folyamatban = false;
		}
	}
</script>

<svelte:head>
	<title>Belépés – Mozi Manager</title>
</svelte:head>

<div class="kartya doboz">
	<h1>Belépés</h1>
	<form class="urlap" onsubmit={bekuld}>
		<label>
			E-mail cím
			<input type="email" bind:value={email} autocomplete="username" required />
		</label>
		<label>
			Jelszó
			<input type="password" bind:value={jelszo} autocomplete="current-password" required />
		</label>
		{#if hiba}
			<div class="uzenet hiba">{hiba}</div>
		{/if}
		<button class="gomb" type="submit" disabled={folyamatban}>
			{folyamatban ? 'Belépés…' : 'Belépés'}
		</button>
	</form>
	<p class="halvany">Nincs még fiókod? <a href="/regisztracio?vissza={vissza}">Regisztrálj!</a></p>
</div>

<style>
	.doboz {
		max-width: 420px;
		margin: 32px auto;
	}
</style>
