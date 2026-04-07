<script lang="ts">
	import { resolve } from '$app/paths';
</script>

<footer class="border-t border-border bg-background px-6 py-12">
	<div class="mx-auto grid max-w-7xl grid-cols-1 gap-10 md:grid-cols-3">
		<!-- Brand -->
		<div class="flex flex-col gap-3">
			<p class="font-serif text-xl font-bold text-foreground">Peelin' Good</p>
			<p class="text-sm leading-relaxed text-muted-foreground">
				Artisan baked goods, made fresh daily. Come find us — or order online.
			</p>
		</div>

		<!-- Links -->
		<div class="flex flex-col gap-2">
			<p class="mb-2 text-xs font-semibold tracking-widest text-muted-foreground uppercase">
				Explore
			</p>
			<a
				href={resolve('/menu/breads')}
				class="text-sm text-foreground transition-colors hover:text-primary">Breads</a
			>
			<a
				href={resolve('/menu/pastries/')}
				class="text-sm text-foreground transition-colors hover:text-primary">Pastries</a
			>
			<a
				href={resolve('/menu/cakes/')}
				class="text-sm text-foreground transition-colors hover:text-primary">Cakes</a
			>
			<a
				href={resolve('/menu/seasonal/')}
				class="text-sm text-foreground transition-colors hover:text-primary">Seasonal</a
			>
		</div>

		<!-- Contact -->
		<div class="flex flex-col gap-2">
			<p class="mb-2 text-xs font-semibold tracking-widest text-muted-foreground uppercase">
				Find Us
			</p>
			<p class="text-sm text-foreground">123 Flour St, Bakery Town</p>
			<p class="text-sm text-foreground">hello@peelingood.com</p>
			<p class="text-sm text-foreground">(555) 123-4567</p>
		</div>
	</div>

	<div
		class="mx-auto mt-10 flex max-w-7xl items-center justify-between border-t border-border pt-6"
	>
		<p class="text-xs text-muted-foreground">© 2026 Peelin' Good. All rights reserved.</p>
		<p class="text-xs text-muted-foreground">Made with care.</p>
	</div>
</footer>
