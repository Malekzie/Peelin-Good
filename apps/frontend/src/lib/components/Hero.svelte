<script lang="ts">
	import { resolve } from '$app/paths';

	// No props needed for static mockup
</script>

<section class="relative overflow-hidden bg-background">
	<!-- Warm texture overlay using CSS gradient -->
	<div
		class="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_at_top_right,_oklch(0.85_0.05_60)_0%,_transparent_60%)]"
	></div>
	<div
		class="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_at_bottom_left,_oklch(0.90_0.03_145)_0%,_transparent_50%)] opacity-40"
	></div>

	<div
		class="relative mx-auto grid max-w-7xl items-center gap-12 px-6 py-28 md:grid-cols-2 md:py-40"
	>
		<!-- Text content -->
		<div class="flex flex-col gap-6">
			<p class="text-sm font-semibold tracking-widest text-primary uppercase">
				Fresh • Artisan • Local
			</p>
			<h1
				class="font-serif text-5xl leading-tight font-bold text-foreground md:text-6xl lg:text-7xl"
			>
				Baked with<br />heart.
			</h1>
			<p class="max-w-md text-lg leading-relaxed text-muted-foreground">
				Every loaf, pastry, and cake at Peelin' Good is made from scratch — with ingredients you can
				trust and flavors you'll remember.
			</p>
			<div class="flex gap-4 pt-2">
				<a
					href={resolve('/menu')}
					class="inline-flex items-center rounded-full bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground transition-opacity hover:opacity-90"
				>
					Browse Menu
				</a>
				<a
					href={resolve('/about')}
					class="inline-flex items-center rounded-full border border-border px-6 py-3 text-sm font-semibold text-foreground transition-colors hover:bg-muted"
				>
					Our Story
				</a>
			</div>
		</div>

		<!-- Decorative visual block (placeholder for image) -->
		<div class="relative hidden items-center justify-center md:flex">
			<div
				class="flex h-96 w-96 items-center justify-center rounded-full bg-[oklch(0.92_0.04_60)] shadow-inner"
			>
				<div class="text-center">
					<p class="text-7xl">🥐</p>
					<p class="mt-4 font-serif text-xl text-foreground/60">est. 2024</p>
				</div>
			</div>
			<!-- Floating accent blobs -->
			<div class="absolute -top-4 -right-4 h-20 w-20 rounded-full bg-primary/20"></div>
			<div class="absolute -bottom-6 -left-6 h-14 w-14 rounded-full bg-accent/30"></div>
		</div>
	</div>
</section>
