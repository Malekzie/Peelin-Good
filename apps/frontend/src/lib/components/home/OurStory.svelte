<script lang="ts">
	import { resolve } from '$app/paths';
</script>

<section class="bg-[#FAF7F2] px-6 py-20">
	<div class="mx-auto grid max-w-7xl grid-cols-1 items-center gap-16 md:grid-cols-2">
		<!-- Image placeholder -->
		<div
			class="flex h-80 items-center justify-center overflow-hidden rounded-xl border border-border bg-[#F5EFE6]"
		>
			<p class="text-sm text-muted-foreground">Photo coming soon</p>
		</div>

		<!-- Text -->
		<div class="flex flex-col gap-4">
			<p class="text-[11px] font-semibold tracking-[0.2em] text-[#C4714A] uppercase">About us</p>

			<h2 class="text-4xl leading-tight font-black tracking-tight text-[#2C1A0E]">
				Baked fresh.<br />
				Made with
				<span class="text-[#C4714A]">care.</span>
			</h2>

			<p class="text-sm leading-relaxed text-muted-foreground">
				At Peelin' Good, everything we make is rooted in simple ingredients and thoughtful
				technique. Our bakery is built on a love for slow baking, quality sourcing, and creating a
				space that feels warm and welcoming.
			</p>

			<p class="text-sm leading-relaxed text-muted-foreground">
				From handcrafted breads to daily pastries, every item is made in small batches with
				attention to detail—no shortcuts, just honest baking.
			</p>

			<a
				href={resolve('/about')}
				class="mt-1 self-start text-sm font-semibold text-[#C4714A] underline decoration-[#C4714A]/40 underline-offset-4 hover:decoration-[#C4714A]"
			>
				Learn more about us →
			</a>
		</div>
	</div>
</section>
