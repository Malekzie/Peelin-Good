<script>
	import { Button } from '$lib/components/ui/button';
</script>

<div class="flex min-h-screen items-center justify-center bg-background px-6">
	<div class="w-full max-w-md rounded-2xl border border-border bg-card p-10 text-center shadow-sm">
		<h1 class="mb-2 text-2xl font-bold text-foreground">Account deactivated</h1>
		<p class="mb-8 text-sm text-muted-foreground">
			Your account has been deactivated. If you think this is a mistake, please contact us and we'll
			get it sorted out.
		</p>
		<Button href="mailto:peelingoodbakery@gmail.com" class="w-full">Contact support</Button>
		<Button href="/" variant="outline" class="mt-3 w-full">Go home</Button>
	</div>
</div>
