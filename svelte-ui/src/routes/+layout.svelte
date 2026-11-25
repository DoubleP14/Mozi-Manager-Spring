<script lang="ts">
	import '../app.css';
	import favicon from '#lib/assets/favicon.svg';
	import { auth } from '#lib/auth.svelte.ts';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import type { LayoutProps } from './$types';

	let { children }: LayoutProps = $props();

	// Indulaskor lekerdezzuk, van-e mar bejelentkezett session
	auth.betolt();

	async function kijelentkezes() {
		await auth.kijelentkezes();
		await goto('/');
	}

	const aktiv = (utvonal: string) =>
		utvonal === '/' ? page.url.pathname === '/' : page.url.pathname.startsWith(utvonal);
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
</svelte:head>

<header>
	<nav class="tartalom">
		<a class="logo" href="/">🎬 Mozi Manager</a>
		<div class="menu">
			<a href="/" class:aktiv={aktiv('/') || aktiv('/vetites')}>Műsor</a>
			{#if auth.felhasznalo}
				<a href="/jegyeim" class:aktiv={aktiv('/jegyeim')}>Jegyeim</a>
			{/if}
		</div>
		<div class="felhasznalo">
			{#if !auth.betoltve}
				<span class="halvany">…</span>
			{:else if auth.felhasznalo}
				<span class="nev">
					{auth.felhasznalo.nev}
					<span class="cimke" title="Törzsvásárlói pontok">★ {auth.felhasznalo.torzsvasarloiPontok} pont</span>
				</span>
				<button class="gomb masodlagos" onclick={kijelentkezes}>Kilépés</button>
			{:else}
				<a class="gomb masodlagos" href="/belepes">Belépés</a>
				<a class="gomb" href="/regisztracio">Regisztráció</a>
			{/if}
		</div>
	</nav>
</header>

<main class="tartalom">
	{@render children()}
</main>

<style>
	header {
		position: sticky;
		top: 0;
		z-index: 10;
		background: color-mix(in srgb, var(--hatter) 88%, transparent);
		backdrop-filter: blur(8px);
		border-bottom: 1px solid var(--keret);
	}

	nav {
		display: flex;
		align-items: center;
		gap: 20px;
		flex-wrap: wrap;
		padding-top: 12px;
		padding-bottom: 12px;
	}

	.logo {
		font-weight: 800;
		font-size: 1.15rem;
		color: var(--szoveg);
	}

	.menu {
		display: flex;
		gap: 16px;
		flex: 1;
	}

	.menu a {
		color: var(--halvany);
	}

	.menu a.aktiv {
		color: var(--szoveg);
		font-weight: 600;
	}

	.felhasznalo {
		display: flex;
		align-items: center;
		gap: 10px;
		flex-wrap: wrap;
	}

	.nev {
		display: flex;
		align-items: center;
		gap: 8px;
	}

	.felhasznalo .gomb {
		padding: 6px 14px;
	}
</style>
