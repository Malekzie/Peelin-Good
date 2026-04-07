<script lang="ts">
	import StaffSidebar from '$lib/components/staff/StaffSidebar.svelte';

	let { children } = $props();
</script>

<div class="flex h-[calc(100dvh-var(--app-navbar-height))] overflow-hidden bg-background">
	<StaffSidebar />
	<div class="flex flex-1 flex-col overflow-hidden">
		{@render children()}
	</div>
</div>
