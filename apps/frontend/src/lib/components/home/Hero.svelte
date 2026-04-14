<script lang="ts">
	import { resolve } from '$app/paths';
	import { Button } from '$lib/components/ui/button/index';
</script>

<section class="grid min-h-125 grid-cols-1 bg-background md:grid-cols-2">
	<!-- Text -->
	<div class="flex flex-col justify-center gap-5 px-8 py-16 md:px-12 lg:px-16">
		<img
			src="/images/Peelin' Good ~BAKERY~.png"
			alt="Peelin' Good Bakery"
			class="w-64 self-center object-contain"
		/>
		<h1 class="text-5xl leading-[1.08] font-black tracking-tight text-foreground lg:text-6xl">
			Made from<br />scratch.<br />
			<span class="text-primary">Always.</span>
		</h1>
		<p class="max-w-sm text-[15px] leading-relaxed text-muted-foreground">
			Bread, pastries, and cakes baked in small batches every morning, with local ingredients and
			nothing to hide.
		</p>
		<div class="flex gap-3 pt-2">
			<Button
				href={resolve('/menu')}
				class="rounded-full bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground transition-opacity hover:opacity-90"
				size="default"
				variant="outline"
			>
				Browse the menu
			</Button>
			<Button
				href={resolve('/about')}
				class="rounded-full border border-border px-6 py-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
				variant="outline"
			>
				Our story
			</Button>
		</div>
	</div>

	<!-- Photo + type overlay panel -->
	<div class="relative hidden overflow-hidden md:block">
		<!-- Unsplash bakery photo -->
		<img
			src="https://peelin-good-storage.tor1.cdn.digitaloceanspaces.com/misc/bakery-header.png"
			alt="Fresh baked goods at Peelin' Good"
			class="absolute inset-0 h-full w-full object-cover brightness-95"
		/>

		<!-- Dark scrim so text is readable -->
		<div class="absolute inset-0 bg-black/45"></div>

		<!-- Typographic overlay -->
		<div class="relative flex h-full flex-col justify-between p-10">
			<div class="leading-none">
				<p class="text-[72px] font-black tracking-tight text-white/90 lg:text-[88px]">BAKED</p>
				<p class="text-[72px] font-black tracking-tight text-primary lg:text-[88px]">FRESH</p>
				<p class="text-[72px] font-black tracking-tight text-white/50 lg:text-[88px]">DAILY</p>
			</div>

			<div class="flex items-end justify-end">
				<a href={resolve('/menu')} class="rounded-full bg-primary px-4 py-2">
					<p class="text-xs font-bold tracking-widest text-primary-foreground uppercase">
						Order today
					</p>
				</a>
			</div>
		</div>
	</div>
</section>
